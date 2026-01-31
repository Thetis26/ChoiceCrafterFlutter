import BackgroundTasks
import FirebaseCore
import FirebaseFirestore
import FirebaseStorage
import Foundation

final class WeeklyUsageExportWorker {
    static let taskIdentifier = "com.choicecrafter.students.weeklyUsageExport"

    private static let experimentAssignmentsCollection = "EXPERIMENT_ASSIGNMENTS"
    private static let experimentMetadataCollection = "EXPERIMENT_METADATA"
    private static let experimentMetadataDocument = "config"
    private static let summaryVersion = "2.0"
    private static let phaseSequence = ["phase1", "phase2", "phase3", "phase4"]
    private static let experimentGroups = ["A", "B"]

    private let firestore: Firestore
    private let storage: Storage
    private let calendar: Calendar
    private let dateFormatter: DateFormatter
    private let isoFormatter: ISO8601DateFormatter
    private let isoFormatterWithFractional: ISO8601DateFormatter

    init() {
        if FirebaseApp.app() == nil {
            FirebaseApp.configure()
        }
        firestore = Firestore.firestore()
        storage = Storage.storage()
        calendar = WeeklyUsageExportWorker.makeCalendar()
        dateFormatter = WeeklyUsageExportWorker.makeDateFormatter()
        isoFormatter = WeeklyUsageExportWorker.makeIsoFormatter()
        isoFormatterWithFractional = WeeklyUsageExportWorker.makeIsoFormatterWithFractional()
    }

    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: taskIdentifier, using: nil) { task in
            guard let processingTask = task as? BGProcessingTask else {
                task.setTaskCompleted(success: false)
                return
            }
            let worker = WeeklyUsageExportWorker()
            worker.handleProcessingTask(processingTask)
        }
    }

    static func schedule() {
        let request = BGProcessingTaskRequest(identifier: taskIdentifier)
        request.requiresNetworkConnectivity = true
        request.earliestBeginDate = Date().addingTimeInterval(7 * 24 * 60 * 60)
        do {
            try BGTaskScheduler.shared.submit(request)
        } catch {
            print("WeeklyUsageExportWorker schedule failed: \(error)")
        }
    }

    private func handleProcessingTask(_ task: BGProcessingTask) {
        WeeklyUsageExportWorker.schedule()
        let operation = Task {
            do {
                try await exportWeeklyUsage()
                task.setTaskCompleted(success: true)
            } catch {
                print("WeeklyUsageExportWorker failed: \(error)")
                task.setTaskCompleted(success: false)
            }
        }
        task.expirationHandler = {
            operation.cancel()
        }
    }

    private func exportWeeklyUsage() async throws {
        let weekWindow = Self.resolveWeekWindow(calendar: calendar)

        let courseSnapshot = try await getDocuments(
            collection: firestore.collection("COURSE_ENROLLMENTS")
        )

        var usageByUser: [String: UserUsageAccumulator] = [:]

        for document in courseSnapshot.documents {
            let data = document.data()
            guard let userId = data["userId"] as? String, !userId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                continue
            }
            guard let progressSummary = data["progressSummary"] as? [String: Any] else {
                continue
            }
            guard let activitySnapshots = progressSummary["activitySnapshots"] as? [Any] else {
                continue
            }

            let accumulator = usageByUser[userId] ?? UserUsageAccumulator(userId: userId, calendar: calendar, dateFormatter: dateFormatter)
            usageByUser[userId] = accumulator

            for entry in activitySnapshots {
                guard let snapshotMap = entry as? [String: Any] else {
                    continue
                }
                let courseId = asString(snapshotMap["courseId"])
                let activityId = asString(snapshotMap["activityId"])
                guard let taskStats = snapshotMap["taskStats"] as? [String: Any] else {
                    continue
                }

                var activityAccumulator: ActivityUsageAccumulator?
                var earliestAttempt: Date?

                for value in taskStats.values {
                    guard let attempt = value as? [String: Any] else {
                        continue
                    }

                    guard let attemptDate = parseAttemptDate(asString(attempt["attemptDateTime"])) else {
                        continue
                    }

                    if earliestAttempt == nil || attemptDate < earliestAttempt! {
                        earliestAttempt = attemptDate
                    }

                    if !Self.isWithinWeek(attemptDate, weekWindow: weekWindow) {
                        continue
                    }

                    if activityAccumulator == nil {
                        activityAccumulator = accumulator.ensureActivityAccumulator(courseId: courseId, activityId: activityId)
                        accumulator.registerActivityParticipation(courseId: courseId)
                    }

                    accumulator.registerAttempt(attemptDate: attemptDate, attempt: attempt)
                    activityAccumulator?.registerAttempt(attemptDate: attemptDate, attempt: attempt)
                }

                if let activityAccumulator = activityAccumulator {
                    let isNewContent = earliestAttempt != nil && Self.isWithinWeek(earliestAttempt!, weekWindow: weekWindow)
                    activityAccumulator.setNewContent(isNewContent)
                }
            }
        }

        let userGroupAssignments = try await fetchUserGroupAssignments()
        let experimentStartDate = try await resolveExperimentStartDate(fallback: weekWindow.start)

        var users: [UserWeeklyUsage] = []
        for accumulator in usageByUser.values where accumulator.hasData {
            let metadata = Self.buildMetadata(
                userId: accumulator.userId,
                weekStart: weekWindow.start,
                experimentStartDate: experimentStartDate,
                assignments: userGroupAssignments
            )
            users.append(accumulator.toWeeklyUsage(metadata: metadata))
        }

        users.sort { $0.userId < $1.userId }

        let summary = Self.buildSummary(weekStart: weekWindow.start, weekEnd: weekWindow.end, users: users, dateFormatter: dateFormatter)
        let jsonPayload = try encodeSummary(summary)
        try await uploadJson(jsonPayload, weekStart: weekWindow.start, weekEnd: weekWindow.end, userCount: users.count)
    }

    private func fetchUserGroupAssignments() async throws -> [String: String] {
        var assignments: [String: String] = [:]
        do {
            let snapshot = try await getDocuments(collection: firestore.collection(Self.experimentAssignmentsCollection))
            for document in snapshot.documents {
                let data = document.data()
                var userId = data["userId"] as? String
                if userId?.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ?? true {
                    userId = document.documentID
                }
                let normalized = Self.normalizeGroup(data["group"] as? String)
                if let userId = userId, let normalized = normalized {
                    assignments[userId] = normalized
                }
            }
        } catch {
            print("WeeklyUsageExportWorker failed to fetch experiment assignments: \(error)")
        }
        return assignments
    }

    private func resolveExperimentStartDate(fallback: Date) async throws -> Date {
        do {
            let document = try await getDocument(
                document: firestore.collection(Self.experimentMetadataCollection).document(Self.experimentMetadataDocument)
            )
            if document.exists, let startDate = document.data()?["startDate"] as? String {
                let trimmed = startDate.trimmingCharacters(in: .whitespacesAndNewlines)
                if !trimmed.isEmpty, let parsed = dateFormatter.date(from: trimmed) {
                    return parsed
                }
            }
        } catch {
            print("WeeklyUsageExportWorker failed to fetch experiment metadata: \(error)")
        }
        return fallback
    }

    private func uploadJson(_ jsonPayload: String, weekStart: Date, weekEnd: Date, userCount: Int) async throws {
        let fileName = "\(dateFormatter.string(from: weekStart))_\(dateFormatter.string(from: weekEnd))_\(userCount).json"
        let reference = storage.reference().child("weekly-usage").child(fileName)
        let data = Data(jsonPayload.utf8)
        try await putData(reference: reference, data: data)
    }

    private func encodeSummary(_ summary: WeeklyUsageSummary) throws -> String {
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        let data = try encoder.encode(summary)
        return String(decoding: data, as: UTF8.self)
    }

    private func getDocuments(collection: CollectionReference) async throws -> QuerySnapshot {
        try await withCheckedThrowingContinuation { continuation in
            collection.getDocuments { snapshot, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else if let snapshot = snapshot {
                    continuation.resume(returning: snapshot)
                } else {
                    continuation.resume(throwing: NSError(domain: "WeeklyUsageExportWorker", code: 1, userInfo: [NSLocalizedDescriptionKey: "Missing snapshot"]))
                }
            }
        }
    }

    private func getDocument(document: DocumentReference) async throws -> DocumentSnapshot {
        try await withCheckedThrowingContinuation { continuation in
            document.getDocument { snapshot, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else if let snapshot = snapshot {
                    continuation.resume(returning: snapshot)
                } else {
                    continuation.resume(throwing: NSError(domain: "WeeklyUsageExportWorker", code: 2, userInfo: [NSLocalizedDescriptionKey: "Missing document"]))
                }
            }
        }
    }

    private func putData(reference: StorageReference, data: Data) async throws {
        try await withCheckedThrowingContinuation { continuation in
            reference.putData(data, metadata: nil) { _, error in
                if let error = error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }

    private func parseAttemptDate(_ attemptDateTime: String?) -> Date? {
        guard let attemptDateTime = attemptDateTime?.trimmingCharacters(in: .whitespacesAndNewlines), !attemptDateTime.isEmpty else {
            return nil
        }
        if let parsed = isoFormatterWithFractional.date(from: attemptDateTime) {
            return parsed
        }
        if let parsed = isoFormatter.date(from: attemptDateTime) {
            return parsed
        }
        let localFormatter = DateFormatter()
        localFormatter.locale = Locale(identifier: "en_US_POSIX")
        localFormatter.timeZone = calendar.timeZone
        localFormatter.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        return localFormatter.date(from: attemptDateTime)
    }

    private func resolveScoreRatio(_ attempt: [String: Any]) -> Double {
        if let number = attempt["scoreRatio"] as? NSNumber {
            return max(0, min(1, number.doubleValue))
        }
        if let ratioString = attempt["scoreRatio"] as? String, let ratio = Double(ratioString) {
            return max(0, min(1, ratio))
        }
        let success = asBoolean(attempt["success"]) ?? false
        return success ? 1.0 : 0.0
    }

    private static func buildSummary(weekStart: Date, weekEnd: Date, users: [UserWeeklyUsage], dateFormatter: DateFormatter) -> WeeklyUsageSummary {
        let collector = AggregateCollector()
        users.forEach { collector.include($0) }
        let generatedAtUtc = ISO8601DateFormatter().string(from: Date())
        return WeeklyUsageSummary(
            version: summaryVersion,
            generatedAtUtc: generatedAtUtc,
            weekStart: dateFormatter.string(from: weekStart),
            weekEnd: dateFormatter.string(from: weekEnd),
            activeUsers: users.count,
            countsByCondition: collector.buildCountsByCondition(),
            countsByPhaseAndGroup: collector.buildCountsByPhaseAndGroup(),
            users: users
        )
    }

    private static func buildMetadata(
        userId: String,
        weekStart: Date,
        experimentStartDate: Date,
        assignments: [String: String]
    ) -> UserWeekMetadata {
        let phase = determineExperimentPhase(experimentStartDate: experimentStartDate, weekStart: weekStart)
        let assigned = assignments[userId]
        var group = normalizeGroup(assigned) ?? "A"
        var groupMissing = false
        if normalizeGroup(assigned) == nil, requiresGroupAssignment(phase: phase) {
            groupMissing = true
        }
        let nudged = computeNudged(phase: phase, group: group)
        return UserWeekMetadata(experimentPhase: phase, group: group, nudged: nudged, groupAssignmentMissing: groupMissing)
    }

    private static func determineExperimentPhase(experimentStartDate: Date, weekStart: Date) -> String {
        let calendar = makeCalendar()
        var weeks = calendar.dateComponents([.weekOfYear], from: experimentStartDate, to: weekStart).weekOfYear ?? 0
        if weeks < 0 {
            weeks = 0
        }
        if weeks < 2 {
            return "phase1"
        }
        if weeks < 4 {
            return "phase2"
        }
        if weeks < 6 {
            return "phase3"
        }
        return "phase4"
    }

    private static func computeNudged(phase: String, group: String) -> Bool {
        if phase == "phase2" {
            return group == "B"
        }
        if phase == "phase3" {
            return group == "A"
        }
        return true
    }

    private static func normalizeGroup(_ group: String?) -> String? {
        guard let group = group?.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() else {
            return nil
        }
        if group == "A" || group == "B" {
            return group
        }
        return nil
    }

    private static func requiresGroupAssignment(phase: String) -> Bool {
        phase == "phase2" || phase == "phase3"
    }

    private static func isWithinWeek(_ value: Date, weekWindow: WeekWindow) -> Bool {
        value >= weekWindow.startInstant && value <= weekWindow.endInstant
    }

    private static func resolveWeekWindow(calendar: Calendar) -> WeekWindow {
        let referenceDate = calendar.date(byAdding: .day, value: -1, to: Date()) ?? Date()
        let weekStart = calendar.date(from: calendar.dateComponents([.yearForWeekOfYear, .weekOfYear], from: referenceDate)) ?? referenceDate
        let weekEnd = calendar.date(byAdding: .day, value: 6, to: weekStart) ?? weekStart
        let startInstant = calendar.startOfDay(for: weekStart)
        let endInstant = calendar.date(byAdding: .day, value: 1, to: weekEnd)?.addingTimeInterval(-0.001) ?? weekEnd
        return WeekWindow(start: weekStart, end: weekEnd, startInstant: startInstant, endInstant: endInstant)
    }

    private static func makeCalendar() -> Calendar {
        var calendar = Calendar(identifier: .iso8601)
        calendar.timeZone = .current
        return calendar
    }

    private static func makeDateFormatter() -> DateFormatter {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = .current
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter
    }

    private static func makeIsoFormatter() -> ISO8601DateFormatter {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return formatter
    }

    private static func makeIsoFormatterWithFractional() -> ISO8601DateFormatter {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }
}

private struct WeekWindow {
    let start: Date
    let end: Date
    let startInstant: Date
    let endInstant: Date
}

private struct UserWeekMetadata {
    let experimentPhase: String
    let group: String
    let nudged: Bool
    let groupAssignmentMissing: Bool
}

private final class UserUsageAccumulator {
    let userId: String
    private let calendar: Calendar
    private let dateFormatter: DateFormatter
    private var activeDays: Set<Date> = []
    private var courseIds: Set<String> = []
    private var activities: [String: ActivityUsageAccumulator] = [:]
    private(set) var tasksAttempted = 0
    private(set) var successfulTasks = 0
    private(set) var totalTimeSeconds = 0
    private(set) var totalRetries = 0
    private(set) var retrySamples = 0
    private(set) var hintsUsed = 0
    private var scoreAccumulator: Double = 0

    init(userId: String, calendar: Calendar, dateFormatter: DateFormatter) {
        self.userId = userId
        self.calendar = calendar
        self.dateFormatter = dateFormatter
    }

    func ensureActivityAccumulator(courseId: String?, activityId: String?) -> ActivityUsageAccumulator {
        let key = "\(courseId ?? "")|\(activityId ?? "")"
        if let existing = activities[key] {
            return existing
        }
        let created = ActivityUsageAccumulator(courseId: courseId, activityId: activityId, calendar: calendar, dateFormatter: dateFormatter)
        activities[key] = created
        return created
    }

    func registerActivityParticipation(courseId: String?) {
        if let courseId = courseId {
            courseIds.insert(courseId)
        }
    }

    func registerAttempt(attemptDate: Date, attempt: [String: Any]) {
        activeDays.insert(calendar.startOfDay(for: attemptDate))
        tasksAttempted += 1

        let scoreRatio = resolveScoreRatio(attempt)
        scoreAccumulator += scoreRatio
        if scoreRatio >= 0.999 {
            successfulTasks += 1
        }

        if let retries = asInteger(attempt["retries"]) {
            totalRetries += retries
            retrySamples += 1
        }

        if asBoolean(attempt["hintsUsed"]) == true {
            hintsUsed += 1
        }

        totalTimeSeconds += parseTimeSpentSeconds(asString(attempt["timeSpent"]))
    }

    var hasData: Bool {
        !activities.isEmpty && tasksAttempted > 0
    }

    func toWeeklyUsage(metadata: UserWeekMetadata) -> UserWeeklyUsage {
        var activitySummaries = activities.values.map { $0.toSummary() }
        activitySummaries.sort {
            let lhsCourse = $0.courseId ?? ""
            let rhsCourse = $1.courseId ?? ""
            if lhsCourse != rhsCourse {
                return lhsCourse < rhsCourse
            }
            let lhsActivity = $0.activityId ?? ""
            let rhsActivity = $1.activityId ?? ""
            return lhsActivity < rhsActivity
        }

        let successRate = tasksAttempted == 0 ? 0.0 : scoreAccumulator / Double(tasksAttempted)
        let averageRetries = retrySamples == 0 ? 0.0 : Double(totalRetries) / Double(retrySamples)
        let newActivities = activitySummaries.filter { $0.newContent }.count

        let sortedDays = activeDays.sorted()
        let activeDayStrings = sortedDays.map { dateFormatter.string(from: $0) }

        return UserWeeklyUsage(
            userId: userId,
            experimentPhase: metadata.experimentPhase,
            group: metadata.group,
            nudged: metadata.nudged,
            groupAssignmentMissing: metadata.groupAssignmentMissing ? true : nil,
            activeDays: activeDayStrings,
            courseCount: courseIds.count,
            areas: activitySummaries,
            tasksAttempted: tasksAttempted,
            successfulTasks: successfulTasks,
            successRate: successRate,
            averageRetries: averageRetries,
            totalTimeSpentSeconds: totalTimeSeconds,
            hintsUsed: hintsUsed,
            newActivitiesExplored: newActivities,
            totalRetries: totalRetries,
            retrySamples: retrySamples
        )
    }

    private func resolveScoreRatio(_ attempt: [String: Any]) -> Double {
        if let number = attempt["scoreRatio"] as? NSNumber {
            return max(0, min(1, number.doubleValue))
        }
        if let ratioString = attempt["scoreRatio"] as? String, let ratio = Double(ratioString) {
            return max(0, min(1, ratio))
        }
        let success = asBoolean(attempt["success"]) ?? false
        return success ? 1.0 : 0.0
    }
}

private final class ActivityUsageAccumulator {
    let courseId: String?
    let activityId: String?
    private let calendar: Calendar
    private let dateFormatter: DateFormatter
    private var tasksAttempted = 0
    private var successfulTasks = 0
    private var totalTimeSeconds = 0
    private var totalRetries = 0
    private var retrySamples = 0
    private var hintsUsed = 0
    private var newContent = false
    private var activeDays: Set<Date> = []
    private var scoreAccumulator: Double = 0

    init(courseId: String?, activityId: String?, calendar: Calendar, dateFormatter: DateFormatter) {
        self.courseId = courseId
        self.activityId = activityId
        self.calendar = calendar
        self.dateFormatter = dateFormatter
    }

    func registerAttempt(attemptDate: Date, attempt: [String: Any]) {
        tasksAttempted += 1
        activeDays.insert(calendar.startOfDay(for: attemptDate))

        let scoreRatio = resolveScoreRatio(attempt)
        scoreAccumulator += scoreRatio
        if scoreRatio >= 0.999 {
            successfulTasks += 1
        }

        if let retries = asInteger(attempt["retries"]) {
            totalRetries += retries
            retrySamples += 1
        }

        if asBoolean(attempt["hintsUsed"]) == true {
            hintsUsed += 1
        }

        totalTimeSeconds += parseTimeSpentSeconds(asString(attempt["timeSpent"]))
    }

    func setNewContent(_ newContent: Bool) {
        self.newContent = self.newContent || newContent
    }

    func toSummary() -> ActivityUsage {
        let successRate = tasksAttempted == 0 ? 0.0 : scoreAccumulator / Double(tasksAttempted)
        let averageRetries = retrySamples == 0 ? 0.0 : Double(totalRetries) / Double(retrySamples)
        let sortedDays = activeDays.sorted()
        let activeDayStrings = sortedDays.map { dateFormatter.string(from: $0) }
        return ActivityUsage(
            courseId: courseId,
            activityId: activityId,
            tasksAttempted: tasksAttempted,
            successfulTasks: successfulTasks,
            successRate: successRate,
            averageRetries: averageRetries,
            totalTimeSpentSeconds: totalTimeSeconds,
            hintsUsed: hintsUsed,
            newContent: newContent,
            activeDays: activeDayStrings
        )
    }

    private func resolveScoreRatio(_ attempt: [String: Any]) -> Double {
        if let number = attempt["scoreRatio"] as? NSNumber {
            return max(0, min(1, number.doubleValue))
        }
        if let ratioString = attempt["scoreRatio"] as? String, let ratio = Double(ratioString) {
            return max(0, min(1, ratio))
        }
        let success = asBoolean(attempt["success"]) ?? false
        return success ? 1.0 : 0.0
    }
}

private struct WeeklyUsageSummary: Codable {
    let version: String
    let generatedAtUtc: String
    let weekStart: String
    let weekEnd: String
    let activeUsers: Int
    let countsByCondition: [String: AggregateMetrics]
    let countsByPhaseAndGroup: [String: [String: AggregateMetrics]]
    let users: [UserWeeklyUsage]
}

private struct UserWeeklyUsage: Codable {
    let userId: String
    let experimentPhase: String
    let group: String
    let nudged: Bool
    let groupAssignmentMissing: Bool?
    let activeDays: [String]
    let courseCount: Int
    let areas: [ActivityUsage]
    let tasksAttempted: Int
    let successfulTasks: Int
    let successRate: Double
    let averageRetries: Double
    let totalTimeSpentSeconds: Int
    let hintsUsed: Int
    let newActivitiesExplored: Int

    let totalRetries: Int
    let retrySamples: Int

    enum CodingKeys: String, CodingKey {
        case userId
        case experimentPhase
        case group
        case nudged
        case groupAssignmentMissing
        case activeDays
        case courseCount
        case areas
        case tasksAttempted
        case successfulTasks
        case successRate
        case averageRetries
        case totalTimeSpentSeconds
        case hintsUsed
        case newActivitiesExplored
    }
}

private struct ActivityUsage: Codable {
    let courseId: String?
    let activityId: String?
    let tasksAttempted: Int
    let successfulTasks: Int
    let successRate: Double
    let averageRetries: Double
    let totalTimeSpentSeconds: Int
    let hintsUsed: Int
    let newContent: Bool
    let activeDays: [String]
}

private struct AggregateMetrics: Codable {
    let activeUsers: Int
    let totalTasks: Int
    let totalTimeSpentSeconds: Int
    let avgRetries: Double
    let hintsUsed: Int
    let successRate: Double
    let nudged: Bool?

    static func fromTotals(_ totals: RunningTotals, nudged: Bool?) -> AggregateMetrics {
        let avgRetries = totals.retrySamples == 0 ? 0.0 : Double(totals.totalRetries) / Double(totals.retrySamples)
        let successRate = totals.totalTasks == 0 ? 0.0 : Double(totals.successfulTasks) / Double(totals.totalTasks)
        return AggregateMetrics(
            activeUsers: totals.activeUsers,
            totalTasks: totals.totalTasks,
            totalTimeSpentSeconds: totals.totalTimeSeconds,
            avgRetries: avgRetries,
            hintsUsed: totals.hintsUsed,
            successRate: successRate,
            nudged: nudged
        )
    }
}

private final class AggregateCollector {
    private var conditionTotals: [Bool: RunningTotals] = [:]
    private var phaseGroupTotals: [String: [String: RunningTotals]] = [:]

    func include(_ usage: UserWeeklyUsage) {
        let nudged = usage.nudged
        let condition = conditionTotals[nudged] ?? RunningTotals()
        condition.include(usage)
        conditionTotals[nudged] = condition

        let phase = usage.experimentPhase
        let group = usage.group
        var groupTotals = phaseGroupTotals[phase] ?? [:]
        let totals = groupTotals[group] ?? RunningTotals()
        totals.include(usage)
        groupTotals[group] = totals
        phaseGroupTotals[phase] = groupTotals
    }

    func buildCountsByCondition() -> [String: AggregateMetrics] {
        var result: [String: AggregateMetrics] = [:]
        result["nudged"] = AggregateMetrics.fromTotals(conditionTotals[true] ?? RunningTotals(), nudged: nil)
        result["nonNudged"] = AggregateMetrics.fromTotals(conditionTotals[false] ?? RunningTotals(), nudged: nil)
        return result
    }

    func buildCountsByPhaseAndGroup() -> [String: [String: AggregateMetrics]] {
        var result: [String: [String: AggregateMetrics]] = [:]
        for phase in WeeklyUsageExportWorker.phaseSequence {
            var groups: [String: AggregateMetrics] = [:]
            let totalsByGroup = phaseGroupTotals[phase] ?? [:]
            for group in WeeklyUsageExportWorker.experimentGroups {
                let totals = totalsByGroup[group] ?? RunningTotals()
                let nudged = WeeklyUsageExportWorker.computeNudged(phase: phase, group: group)
                groups[group] = AggregateMetrics.fromTotals(totals, nudged: nudged)
            }
            result[phase] = groups
        }
        return result
    }
}

private final class RunningTotals {
    var activeUsers = 0
    var totalTasks = 0
    var successfulTasks = 0
    var totalTimeSeconds = 0
    var totalRetries = 0
    var retrySamples = 0
    var hintsUsed = 0

    func include(_ usage: UserWeeklyUsage) {
        activeUsers += 1
        totalTasks += usage.tasksAttempted
        successfulTasks += usage.successfulTasks
        totalTimeSeconds += usage.totalTimeSpentSeconds
        totalRetries += usage.totalRetries
        retrySamples += usage.retrySamples
        hintsUsed += usage.hintsUsed
    }
}

private func asString(_ value: Any?) -> String? {
    guard let value = value else {
        return nil
    }
    if let stringValue = value as? String {
        return stringValue
    }
    return String(describing: value)
}

private func asBoolean(_ value: Any?) -> Bool? {
    if let boolValue = value as? Bool {
        return boolValue
    }
    if let stringValue = value as? String {
        return Bool(stringValue)
    }
    if let numberValue = value as? NSNumber {
        return numberValue.intValue != 0
    }
    return nil
}

private func asInteger(_ value: Any?) -> Int? {
    if let numberValue = value as? NSNumber {
        return numberValue.intValue
    }
    if let stringValue = value as? String {
        return Int(stringValue)
    }
    return nil
}

private func parseTimeSpentSeconds(_ raw: String?) -> Int {
    guard let raw = raw?.trimmingCharacters(in: .whitespacesAndNewlines), !raw.isEmpty else {
        return 0
    }
    let parts = raw.split(separator: ":")
    var units = [0, 0, 0]
    var index = parts.count - 1
    var unitIndex = 2
    while index >= 0 && unitIndex >= 0 {
        units[unitIndex] = Int(parts[index]) ?? 0
        index -= 1
        unitIndex -= 1
    }
    return units[0] * 3600 + units[1] * 60 + units[2]
}
