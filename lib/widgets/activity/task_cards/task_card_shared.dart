import 'package:flutter/material.dart';

import '../../../models/task.dart';
import 'task_type_style.dart';

String taskRewardText(Task task, String fallback) {
  return task.status.isNotEmpty ? task.status : fallback;
}

Widget buildTaskHeader(
  TaskTypeStyle style,
  String rewardText, {
  required Color background,
}) {
  return Row(
    children: [
      Container(
        padding: const EdgeInsets.all(10),
        decoration: BoxDecoration(
          color: background,
          shape: BoxShape.circle,
        ),
        child: Icon(style.icon, color: style.color, size: 20),
      ),
      const SizedBox(width: 12),
      Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            style.label.toUpperCase(),
            style: TextStyle(
              color: Colors.blueGrey.shade700,
              fontWeight: FontWeight.w600,
              letterSpacing: 0.6,
            ),
          ),
          const SizedBox(height: 2),
          Text(
            rewardText,
            style: TextStyle(color: Colors.blueGrey.shade400),
          ),
        ],
      ),
    ],
  );
}

void _showTaskSnackBar(BuildContext context, String message) {
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(SnackBar(content: Text(message)));
}

Widget buildTaskActions(
  BuildContext context, {
  VoidCallback? onHint,
  VoidCallback? onCheck,
}) {
  final Color buttonColor = const Color(0xFF6E7BF2);
  return Row(
    children: [
      Expanded(
        child: ElevatedButton(
          onPressed: onHint ??
              () => _showTaskSnackBar(context, 'Hints will be available soon.'),
          style: ElevatedButton.styleFrom(
            backgroundColor: buttonColor,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 14),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(24),
            ),
            textStyle: const TextStyle(fontWeight: FontWeight.w600),
          ),
          child: const Text('Hint'),
        ),
      ),
      const SizedBox(width: 16),
      Expanded(
        child: ElevatedButton(
          onPressed: onCheck ??
              () => _showTaskSnackBar(context, 'Select an answer to check it.'),
          style: ElevatedButton.styleFrom(
            backgroundColor: buttonColor,
            foregroundColor: Colors.white,
            padding: const EdgeInsets.symmetric(vertical: 14),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(24),
            ),
            textStyle: const TextStyle(fontWeight: FontWeight.w600),
          ),
          child: const Text('Check Answer'),
        ),
      ),
    ],
  );
}

Widget buildTrueFalseOption(
  String label,
  Color accentColor, {
  required bool selected,
  VoidCallback? onTap,
}) {
  return InkWell(
    borderRadius: BorderRadius.circular(14),
    onTap: onTap,
    child: Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      child: Row(
        children: [
          Icon(
            selected ? Icons.radio_button_checked : Icons.radio_button_unchecked,
            color: accentColor,
          ),
          const SizedBox(width: 12),
          Text(label, style: const TextStyle(fontSize: 16)),
        ],
      ),
    ),
  );
}

Widget buildOrderingItem(String label, {Widget? trailing}) {
  return Container(
    padding: const EdgeInsets.symmetric(horizontal: 18, vertical: 16),
    decoration: BoxDecoration(
      color: const Color(0xFFBFCBF9),
      borderRadius: BorderRadius.circular(18),
    ),
    child: Row(
      children: [
        Expanded(
          child: Text(
            label,
            style: const TextStyle(
              color: Colors.white,
              fontWeight: FontWeight.w600,
            ),
          ),
        ),
        trailing ??
            const Icon(Icons.drag_handle, color: Color(0xFFFFD166), size: 20),
      ],
    ),
  );
}

Widget buildBlankInputField({
  required TextEditingController controller,
  required String hintText,
}) {
  return TextField(
    controller: controller,
    decoration: InputDecoration(
      hintText: hintText,
      contentPadding: const EdgeInsets.symmetric(horizontal: 18, vertical: 14),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: BorderSide(color: Colors.blueGrey.shade200, width: 1.4),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(16),
        borderSide: BorderSide(color: Colors.blueGrey.shade200, width: 1.4),
      ),
    ),
  );
}

Widget buildChoiceOption(
  String label,
  Color accentColor, {
  required bool selected,
  VoidCallback? onTap,
}) {
  return InkWell(
    borderRadius: BorderRadius.circular(14),
    onTap: onTap,
    child: Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Icon(
            selected ? Icons.radio_button_checked : Icons.radio_button_unchecked,
            color: accentColor,
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Text(label, style: const TextStyle(fontSize: 16)),
          ),
        ],
      ),
    ),
  );
}
