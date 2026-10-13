package com.example.asalaga1.util;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import com.bumptech.glide.Glide;
import com.example.asalaga1.R;

public class ImageHelper {
    
    /**
     * Resolves an image path/ID into a complete Glide-compatible URL.
     * Supports raw Google Drive image IDs, full HTTP/HTTPS URLs, and fallback default images.
     * 
     * @param imgId the image path or ID from database
     * @return a complete, valid URL string
     */
    public static String getImageUrl(String imgId) {
        if (imgId == null || imgId.isEmpty() || "venue_placeholder".equals(imgId)) {
            return "https://lh3.googleusercontent.com/d/1xr4ibY3QgpwN4Cw0sVHTMA9UsDfgOptz";
        }
        if (imgId.startsWith("http://") || imgId.startsWith("https://")) {
            return imgId;
        }
        return "https://lh3.googleusercontent.com/d/" + imgId;
    }

    /**
     * Shows a dialog displaying a large preview of the payment proof image.
     */
    public static void showImagePreviewDialog(Context context, String imagePath) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_view_image, null);
        ImageView imgView = dialogView.findViewById(R.id.img_dialog_view_proof);
        Button btnClose = dialogView.findViewById(R.id.btn_dialog_view_close);

        String fullUrl = getImageUrl(imagePath);
        Glide.with(context)
             .load(fullUrl)
             .placeholder(R.drawable.ic_booking)
             .error(R.drawable.ic_booking)
             .into(imgView);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
