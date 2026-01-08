class SupportingContent {
  const SupportingContent({this.text, this.imageUrl});

  final String? text;
  final String? imageUrl;
}

class CodingChallengeExample {
  const CodingChallengeExample({
    required this.input,
    required this.output,
    this.explanation,
  });

  final String input;
  final String output;
  final String? explanation;
}

abstract class Task {
  const Task({
    required this.id,
    required this.title,
    required this.description,
    required this.type,
    required this.status,
    this.explanation,
  });

  final String id;
  final String title;
  final String description;
  final String type;
  final String status;
  final String? explanation;

  static Task? fromMap(Map<String, dynamic> map) {
    final rawType = map['type']?.toString();
    if (rawType == null) {
      return null;
    }
    final normalized = rawType.trim().toLowerCase();
    if (normalized.isEmpty) {
      return null;
    }

    switch (normalized) {
      case 'multiple_choice':
      case 'multiplechoice':
      case 'multiple choice':
        return MultipleChoiceTask.fromMap(map, rawType);
      case 'fill_in_the_blank':
      case 'fillintheblank':
      case 'fill in the blank':
        return FillInTheBlankTask.fromMap(map, rawType);
      case 'matching_pair':
      case 'matchingpair':
      case 'matching pair':
        return MatchingPairTask.fromMap(map, rawType);
      case 'ordering':
        return OrderingTask.fromMap(map, rawType);
      case 'info_card':
      case 'infocard':
      case 'info card':
        return InfoCardTask.fromMap(map, rawType);
      case 'true_false':
      case 'truefalse':
      case 'true_or_false':
      case 'true or false':
        return TrueFalseTask.fromMap(map, rawType);
      case 'spot_the_error':
      case 'spoterror':
      case 'spot_error':
      case 'spot the error':
        return SpotTheErrorTask.fromMap(map, rawType);
      case 'coding_challenge':
      case 'codingchallenge':
      case 'coding challenge':
        return CodingChallengeTask.fromMap(map, rawType);
      default:
        return null;
    }
  }
}

class MultipleChoiceTask extends Task {
  const MultipleChoiceTask({
    required super.id,
    required super.title,
    required super.description,
    required super.type,
    required super.status,
    super.explanation,
    required this.question,
    required this.options,
    required this.correctAnswer,
    this.supportingContent,
  });

  final String question;
  final List<String> options;
  final int? correctAnswer;
  final SupportingContent? supportingContent;

  factory MultipleChoiceTask.fromMap(Map<String, dynamic> map, String rawType) {
    return MultipleChoiceTask(
      id: _taskId(map),
      title: _stringValue(map['title']) ?? _stringValue(map['name']) ?? '',
      description: _stringValue(map['description']) ?? '',
      type: rawType,
      status: _stringValue(map['status']) ?? '',
      explanation: _stringValue(map['explanation']),
      question: _stringValue(map['question']) ?? _stringValue(map['prompt']) ?? '',
      options: _stringList(map['options']),
      correctAnswer: _intValue(map['correctAnswer']),
      supportingContent: _supportingContent(map),
    );
  }
}

class FillInTheBlankTask extends Task {
  const FillInTheBlankTask({
    required super.id,
    required super.title,
    required super.description,
    required super.type,
    required super.status,
    super.explanation,
    required this.text,
    required this.missingSegments,
    required this.segmentPositions,
    this.supportingContent,
  });

  final String text;
  final List<String> missingSegments;
  final List<int> segmentPositions;
  final SupportingContent? supportingContent;

  factory FillInTheBlankTask.fromMap(Map<String, dynamic> map, String rawType) {
    return FillInTheBlankTask(
      id: _taskId(map),
      title: _stringValue(map['title']) ?? _stringValue(map['name']) ?? '',
      description: _stringValue(map['description']) ?? '',
      type: rawType,
      status: _stringValue(map['status']) ?? '',
      explanation: _stringValue(map['explanation']),
      text: _stringValue(map['text']) ?? _stringValue(map['blankedText']) ?? '',
      missingSegments: _stringList(map['missingSegments']),
      segmentPositions: _intList(map['segmentPositions']),
      supportingContent: _supportingContent(map),
    );
  }
}

class MatchingPairTask extends Task {
  const MatchingPairTask({
    required super.id,
    required super.title,
    required super.description,
    required super.type,
    required super.status,
    super.explanation,
    required this.leftItems,
    required this.rightItems,
    required this.correctMatches,
  });

  final List<String> leftItems;
  final List<String> rightItems;
  final Map<String, String> correctMatches;

  factory MatchingPairTask.fromMap(Map<String, dynamic> map, String rawType) {
    return MatchingPairTask(
      id: _taskId(map),
      title: _stringValue(map['title']) ?? _stringValue(map['name']) ?? '',
      description: _stringValue(map['description']) ?? '',
      type: rawType,
      status: _stringValue(map['status']) ?? '',
      explanation: _stringValue(map['explanation']),
      leftItems: _stringList(map['leftItems']),
      rightItems: _stringList(map['rightItems']),
      correctMatches: _stringMap(map['correctMatches']),
    );
  }
}

class OrderingTask extends Task {
  const OrderingTask({
    required super.id,
    required super.title,
    required super.description,
    required super.type,
    required super.status,
    super.explanation,
    required this.items,
    required this.correctOrder,
  });

  final List<String> items;
  final List<int> correctOrder;

  factory OrderingTask.fromMap(Map<String, dynamic> map, String rawType) {
    return OrderingTask(
      id: _taskId(map),
      title: _stringValue(map['title']) ?? _stringValue(map['name']) ?? '',
      description: _stringValue(map['description']) ?? '',
      type: rawType,
      status: _stringValue(map['status']) ?? '',
      explanation: _stringValue(map['explanation']),
      items: _stringList(map['items']),
      correctOrder: _intList(map['correctOrder']),
    );
  }
}

class TrueFalseTask extends Task {
  const TrueFalseTask({
    required super.id,
    required super.title,
    required super.description,
    required super.type,
    required super.status,
    super.explanation,
    required this.statement,
    required this.correctAnswer,
  });

  final String statement;
  final bool? correctAnswer;

  factory TrueFalseTask.fromMap(Map<String, dynamic> map, String rawType) {
    return TrueFalseTask(
      id: _taskId(map),
      title: _stringValue(map['title']) ?? _stringValue(map['name']) ?? '',
      description: _stringValue(map['description']) ?? '',
      type: rawType,
      status: _stringValue(map['status']) ?? '',
      explanation: _stringValue(map['explanation']),
      statement: _stringValue(map['statement']) ?? '',
      correctAnswer: _boolValue(map['correctAnswer']),
    );
  }
}

class SpotTheErrorTask extends Task {
  const SpotTheErrorTask({
    required super.id,
    required super.title,
    required super.description,
    required super.type,
    required super.status,
    super.explanation,
    required this.prompt,
    required this.codeSnippet,
    required this.options,
    required this.correctOptionIndex,
  });

  final String prompt;
  final String codeSnippet;
  final List<String> options;
  final int? correctOptionIndex;

  factory SpotTheErrorTask.fromMap(Map<String, dynamic> map, String rawType) {
    return SpotTheErrorTask(
      id: _taskId(map),
      title: _stringValue(map['title']) ?? _stringValue(map['name']) ?? '',
      description: _stringValue(map['description']) ?? '',
      type: rawType,
      status: _stringValue(map['status']) ?? '',
      explanation: _stringValue(map['explanation']),
      prompt: _stringValue(map['prompt']) ?? '',
      codeSnippet: _stringValue(map['codeSnippet']) ?? _stringValue(map['snippet']) ?? '',
      options: _stringList(map['options']),
      correctOptionIndex: _intValue(map['correctOptionIndex']) ??
          _intValue(map['correctAnswer']),
    );
  }
}

class CodingChallengeTask extends Task {
  const CodingChallengeTask({
    required super.id,
    required super.title,
    required super.description,
    required super.type,
    required super.status,
    super.explanation,
    required this.problemDescription,
    required this.expectedOutputDescription,
    required this.examples,
    required this.starterCodeByLanguage,
    required this.solutionCodeByLanguage,
    required this.solutionInput,
    required this.defaultLanguage,
  });

  final String problemDescription;
  final String expectedOutputDescription;
  final List<CodingChallengeExample> examples;
  final Map<String, String> starterCodeByLanguage;
  final Map<String, String> solutionCodeByLanguage;
  final String solutionInput;
  final String defaultLanguage;

  factory CodingChallengeTask.fromMap(Map<String, dynamic> map, String rawType) {
    return CodingChallengeTask(
      id: _taskId(map),
      title: _stringValue(map['title']) ?? _stringValue(map['name']) ?? '',
      description: _stringValue(map['description']) ?? '',
      type: rawType,
      status: _stringValue(map['status']) ?? '',
      explanation: _stringValue(map['explanation']),
      problemDescription: _stringValue(map['problemDescription']) ??
          _stringValue(map['problem']) ??
          '',
      expectedOutputDescription: _stringValue(map['expectedOutput']) ??
          _stringValue(map['expectedOutputDescription']) ??
          '',
      examples: _exampleList(map['examples']),
      starterCodeByLanguage: _stringMap(map['starterCode'])
        ..addAll(_stringMap(map['starterTemplates'])),
      solutionCodeByLanguage: _stringMap(map['solutionCode'])
        ..addAll(_stringMap(map['solutions'])),
      solutionInput: _stringValue(map['solutionInput']) ?? '',
      defaultLanguage: _stringValue(map['defaultLanguage']) ?? '',
    );
  }
}

class InfoCardTask extends Task {
  const InfoCardTask({
    required super.id,
    required super.title,
    required super.description,
    required super.type,
    required super.status,
    super.explanation,
    required this.contentType,
    required this.contentText,
    required this.mediaUrl,
    required this.interactiveUrl,
    required this.actionText,
  });

  final String contentType;
  final String contentText;
  final String mediaUrl;
  final String interactiveUrl;
  final String actionText;

  factory InfoCardTask.fromMap(Map<String, dynamic> map, String rawType) {
    return InfoCardTask(
      id: _taskId(map),
      title: _stringValue(map['title']) ?? _stringValue(map['name']) ?? '',
      description: _stringValue(map['description']) ?? '',
      type: rawType,
      status: _stringValue(map['status']) ?? '',
      explanation: _stringValue(map['explanation']),
      contentType: _stringValue(map['contentType']) ?? '',
      contentText: _stringValue(map['contentText']) ?? '',
      mediaUrl: _stringValue(map['mediaUrl']) ?? '',
      interactiveUrl: _stringValue(map['interactiveUrl']) ?? '',
      actionText: _stringValue(map['actionText']) ?? '',
    );
  }
}

String _taskId(Map<String, dynamic> map) {
  return _stringValue(map['id']) ??
      _stringValue(map['taskId']) ??
      _stringValue(map['task_id']) ??
      '';
}

String? _stringValue(dynamic value) {
  if (value == null) {
    return null;
  }
  final stringValue = value.toString();
  return stringValue.isEmpty ? null : stringValue;
}

int? _intValue(dynamic value) {
  if (value == null) {
    return null;
  }
  if (value is int) {
    return value;
  }
  if (value is num) {
    return value.toInt();
  }
  return int.tryParse(value.toString());
}

bool? _boolValue(dynamic value) {
  if (value == null) {
    return null;
  }
  if (value is bool) {
    return value;
  }
  if (value is num) {
    return value != 0;
  }
  final normalized = value.toString().toLowerCase();
  if (normalized == 'true') {
    return true;
  }
  if (normalized == 'false') {
    return false;
  }
  return null;
}

List<String> _stringList(dynamic value) {
  if (value is List) {
    return value
        .where((entry) => entry != null)
        .map((entry) => entry.toString())
        .toList();
  }
  return [];
}

List<int> _intList(dynamic value) {
  if (value is List) {
    return value
        .map(_intValue)
        .whereType<int>()
        .toList();
  }
  return [];
}

Map<String, String> _stringMap(dynamic value) {
  if (value is Map) {
    return value.map((key, entry) => MapEntry(
          key.toString(),
          entry?.toString() ?? '',
        ));
  }
  return {};
}

SupportingContent? _supportingContent(Map<String, dynamic> map) {
  if (map['supportingContent'] is Map) {
    final contentMap =
        Map<String, dynamic>.from(map['supportingContent'] as Map);
    return SupportingContent(
      text: _stringValue(contentMap['text']),
      imageUrl: _stringValue(contentMap['imageUrl']),
    );
  }

  final supportingText = _stringValue(map['supportingText']);
  final supportingImageUrl = _stringValue(map['supportingImageUrl']) ??
      _stringValue(map['supportingImage']);
  if (supportingText == null && supportingImageUrl == null) {
    return null;
  }
  return SupportingContent(text: supportingText, imageUrl: supportingImageUrl);
}

List<CodingChallengeExample> _exampleList(dynamic value) {
  if (value is List) {
    return value
        .whereType<Map>()
        .map((entry) {
          final map = Map<String, dynamic>.from(entry);
          return CodingChallengeExample(
            input: _stringValue(map['input']) ?? '',
            output: _stringValue(map['output']) ?? '',
            explanation: _stringValue(map['explanation']),
          );
        })
        .toList();
  }
  return [];
}
