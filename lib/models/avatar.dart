class Avatar {
  const Avatar({
    required this.name,
    this.imageUrl,
  });

  final String name;
  final String? imageUrl;

  String? resolvedImageUrl() {
    if (imageUrl == null || imageUrl!.trim().isEmpty) {
      return null;
    }
    if (imageUrl == _legacyPlaceholderUrl) {
      return null;
    }
    return imageUrl;
  }

  static const String _legacyPlaceholderUrl =
      'https://example.com/default_avatar.png';
}
