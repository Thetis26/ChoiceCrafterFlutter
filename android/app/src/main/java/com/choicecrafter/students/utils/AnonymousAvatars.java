package com.choicecrafter.studentapp.utils;

import android.util.Log;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class AnonymousAvatars {

    public interface AvatarsCallback {
        void onAvatarsLoaded(List<Avatar> avatars);
    }

    public void getAllAvatars(final AvatarsCallback callback) {

        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference avatarsRef = storage.getReference().child("avatars"); // folder name

        Log.i("AnonymousAvatars", "Getting avatars from " + avatarsRef.getBucket() +  " " +avatarsRef.getPath());
        avatarsRef.listAll().addOnSuccessListener(listResult -> {
                    List<Avatar> avatarList = new ArrayList<>();
                    List<StorageReference> items = listResult.getItems();
                    Log.i("AnonymousAvatars", "Found " + items.size() + " avatars");
                    if (items.isEmpty()) {
                        Log.e("AnonymousAvatars", "No avatars found");
                        callback.onAvatarsLoaded(avatarList);
                        return;
                    }

                    final int[] count = {0};
                    for (StorageReference item : items) {
                        item.getDownloadUrl().addOnSuccessListener(uri -> {
                            Log.i("AnonymousAvatars", "Found avatar " + item.getName());
                            String name = stripFileExtension(item.getName());
                            String url = uri.toString();
                            avatarList.add(new Avatar(name, url));
                            if (++count[0] == items.size()) {
                                callback.onAvatarsLoaded(avatarList);
                            }
                        }).addOnFailureListener(e -> {
                            Log.e("AnonymousAvatars", "Failed to get avatar " + item.getName(), e);
                            if (++count[0] == items.size()) {
                                callback.onAvatarsLoaded(avatarList);
                            }
                        });
                    }
                })
                .addOnFailureListener(failure -> {
                    Log.e("AnonymousAvatars", "Error getting avatars", failure);
                    callback.onAvatarsLoaded(new ArrayList<>()); // Return empty list on failure
                });
    }

    public void getAvatarByUrl(String url, final AvatarsCallback callback) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference avatarRef = storage.getReferenceFromUrl(url);

        avatarRef.getDownloadUrl().addOnSuccessListener(uri -> {
            String name = stripFileExtension(avatarRef.getName());
            String imageUrl = uri.toString();
            List<Avatar> avatarList = new ArrayList<>();
            avatarList.add(new Avatar(name, imageUrl));
            callback.onAvatarsLoaded(avatarList);
        }).addOnFailureListener(e -> {
            callback.onAvatarsLoaded(new ArrayList<>()); // Return empty list on failure
        });
    }

    private String stripFileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }

        return fileName;
    }
}
