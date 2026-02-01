enum RecommendationType { website, youtube, pdf, doc }

class Recommendation {
  const Recommendation({
    required this.title,
    required this.url,
    required this.description,
    required this.type,
    this.rawType,
  });

  final String title;
  final String url;
  final String description;
  final RecommendationType type;
  final String? rawType;

  factory Recommendation.fromMap(Map<String, dynamic> map) {
    final url = (map['url'] as String?)?.trim() ?? '';
    final typeString = (map['type'] as String?)?.trim().toLowerCase();
    return Recommendation(
      title: (map['title'] as String?)?.trim().isNotEmpty == true
          ? (map['title'] as String).trim()
          : 'Recommendation',
      url: url,
      description: (map['description'] as String?)?.trim() ?? '',
      type: _parseType(typeString, url),
      rawType: typeString,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'title': title,
      'url': url,
      'description': description,
      'type': rawType ?? type.name,
    };
  }

  static RecommendationType _parseType(String? rawType, String url) {
    switch (rawType) {
      case 'youtube':
      case 'video':
        return RecommendationType.youtube;
      case 'pdf':
        return RecommendationType.pdf;
      case 'doc':
      case 'docx':
        return RecommendationType.doc;
      case 'website':
      case 'web':
        return RecommendationType.website;
      default:
        return _inferTypeFromUrl(url);
    }
  }

  static RecommendationType _inferTypeFromUrl(String url) {
    final normalized = url.toLowerCase();
    if (normalized.contains('youtube.com') || normalized.contains('youtu.be')) {
      return RecommendationType.youtube;
    }
    if (normalized.endsWith('.pdf')) {
      return RecommendationType.pdf;
    }
    if (normalized.endsWith('.doc') || normalized.endsWith('.docx')) {
      return RecommendationType.doc;
    }
    return RecommendationType.website;
  }
}
