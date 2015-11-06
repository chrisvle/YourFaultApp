package me.chrisvle.yourfaultapp;

/**
 * Created by Chris on 10/16/15.
 */

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

// Thank you Stackoverflow
public class CustomListAdapter extends ArrayAdapter<String> {

    private final Activity context;
    private final ArrayList<String> itemname;
    private final ArrayList<String> mags;
    private final ArrayList<String> distances;
    private final Integer[] imgid = {R.drawable.low, R.drawable.medium, R.drawable.high};

    public CustomListAdapter(Activity context, ArrayList<String> itemname, ArrayList<String> mags, ArrayList<String> distances) {
        super(context, R.layout.mylist, itemname);
        this.context = context;
        this.itemname = itemname;
        this.mags = mags;
        this.distances = distances;
    }

    public View getView(int position,View view,ViewGroup parent) {
        LayoutInflater inflater = context.getLayoutInflater();
        View rowView = inflater.inflate(R.layout.mylist, null,true);

        TextView txtTitle = (TextView) rowView.findViewById(R.id.item);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        TextView extratxt = (TextView) rowView.findViewById(R.id.textView1);

        txtTitle.setText(itemname.get(position));
        extratxt.setText("Distance: " + distances.get(position) + " M    " + "Magnitude: " + mags.get(position));

        double mag = Double.parseDouble(mags.get(position));
        if (mag < 1.0) {
            rowView.setBackgroundColor(Color.parseColor("#FFDE00"));
            imageView.setImageResource(imgid[0]);
        } else if (mag < 2.0) {
            rowView.setBackgroundColor(Color.parseColor("#FF6900"));
            imageView.setImageResource(imgid[1]);
        } else {
            rowView.setBackgroundColor(Color.parseColor("#F44336"));
            imageView.setImageResource(imgid[2]);
        }
        return rowView;
    };

    public String getItemName(int position) {
       return itemname.get(position);
    }
}