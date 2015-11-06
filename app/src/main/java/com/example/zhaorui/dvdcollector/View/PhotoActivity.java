package com.example.zhaorui.dvdcollector.View;

/*
 * Created by Zhaorui Chen
 * Credit given to https://github.com/CMPUT301W15T06
 *
*/

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.example.zhaorui.dvdcollector.Controller.DVDController;
import com.example.zhaorui.dvdcollector.Controller.GalleryController;
import com.example.zhaorui.dvdcollector.Controller.InventoryController;
import com.example.zhaorui.dvdcollector.Model.DVD;
import com.example.zhaorui.dvdcollector.Model.Gallery;
import com.example.zhaorui.dvdcollector.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

public class PhotoActivity extends BaseActivity{
    private int position;
    private InventoryController ic = new InventoryController();
    private DVDController dc = new DVDController();
    private Gallery gallery;
    private GalleryController gc;

    private int numPhotos;
    private ArrayList<Integer> indexes;
    private ArrayAdapter<Integer> adapter;

    private ListView listView;

    private static final int TAKE_PHOTO = 22;
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);
        listView = (ListView)findViewById(R.id.listView_photos);

        Intent intent = getIntent();
        position = intent.getIntExtra("position", -1);
        ArrayList<String> info = dc.read(ic.get(position));//get the current dvd

        // check if there is photos for this dvd
        if (info.get(4)=="Yes") {
            this.gallery = dc.readPhoto(ic.get(position));//get its gallery
            this.gc = new GalleryController(gallery);
        }else{
            this.gallery = new Gallery();
            this.gc = new GalleryController(gallery);
        }

        // initialize the listview, each entry is provided with an index of image
        numPhotos = gallery.getSize();
        indexes = new ArrayList<Integer>();
        for (int i=0;i<numPhotos;i++){
            indexes.add(i);
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id) {
                // if click on the listview item, show the image on a new activity
                Intent i = new Intent(PhotoActivity.this, DisplayPhotoActivity.class);
                String photoStrToShow = gallery.getPhotoStrs().get(position);
                i.putExtra("photoStr", photoStrToShow);
                startActivity(i);
            }
        });

        // remove the photo
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                int index = indexes.get(position);
                gc.removeFromGallery(gallery.getPhotoStrs().get(position)); // remove the image from the gallery
                dc.changeGallery(ic.get(position), gallery);// change the dvd with the new gallery

                Log.e("DVD", String.valueOf(position));
                Log.e("DVDPhoto", String.valueOf(index));
                // update the listview
                indexes.remove(position);
                adapter.notifyDataSetChanged();

                return true;
            }
        });
    }

    @Override
    protected void onStart(){
        super.onStart();
        adapter = new ArrayAdapter<Integer>(PhotoActivity.this, android.R.layout.simple_list_item_1, indexes);
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    //https://github.com/CMPUT301W15T06/Project/blob/master/App/src/ca/ualberta/CMPUT301W15T06/ClaimantReceiptActivity.java
    // Modified: Zhaorui CHEN
    public void addPhoto(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // create a path for storing the photograph
        String folder = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tmp";
        File folderF = new File(folder);
        if (!folderF.exists()) {
            folderF.mkdir();
        }

        String imageFilePath = folder + "/" + String.valueOf(System.currentTimeMillis()) + ".jpeg";
        File imageFile = new File(imageFilePath);
        imageUri = Uri.fromFile(imageFile);

        // start camera
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(intent, TAKE_PHOTO);
    }


    //https://github.com/CMPUT301W15T06/Project/blob/master/App/src/ca/ualberta/CMPUT301W15T06/ClaimantReceiptActivity.java
    // Modified: Zhaorui CHEN
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case TAKE_PHOTO:
                if (resultCode == RESULT_OK) {
                    try {

                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        String photo = gc.encodeFromBitmap(bitmap); // encode image to string
                        gc.addToGallery(photo); // add the photo to the gallery
                        dc.changeGallery(ic.get(position), gallery);// change the dvd with the new gallery

                        // update the listview
                        numPhotos = gallery.getSize();
                        indexes = new ArrayList<Integer>();
                        for (int i=0;i<numPhotos;i++){
                            indexes.add(i);
                        }
                        adapter.notifyDataSetChanged(); //update the listview

                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(getApplicationContext(), "Take photo canceled!", Toast.LENGTH_LONG).show();
                }
        }
    }

}
