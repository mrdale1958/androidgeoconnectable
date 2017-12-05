package com.botherconsulting.geoconnectable;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.google.android.gms.maps.model.Tile;
import com.google.android.gms.maps.model.TileProvider;

import java.io.ByteArrayOutputStream;

/**
 * Created by dalemacdonald on 12/4/17.
 */

public class DeadTileProvider implements TileProvider {
    Tile emptyTile;

    public DeadTileProvider(Drawable drawable) {

        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] bitmapdata = stream.toByteArray();
        emptyTile = new Tile( 256, 256, bitmapdata);


    }
    @Override
    public  Tile getTile(int x, int y, int zoom) {
        if (zoom > 15) return emptyTile;
        return TileProvider.NO_TILE;

    }
}
