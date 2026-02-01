import 'badge_definition.dart';

class BadgeStatus {
  const BadgeStatus({
    required this.definition,
    required this.earned,
  });

  final BadgeDefinition definition;
  final bool earned;
}
