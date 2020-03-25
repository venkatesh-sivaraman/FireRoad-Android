package com.base12innovations.android.fireroad.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.doc.Semester;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class SelectSemesterAdapter extends RecyclerView.Adapter<SelectSemesterAdapter.ViewHolder>{

    public interface Delegate{
        void selectSemester(Semester semester);
        boolean courseInSemester(Semester semester);
        boolean courseOfferedInSemester(Semester semester);
    }

    public WeakReference<Delegate> delegate;
    public int numYears=0;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        final LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        return new ViewHolder(layoutInflater.inflate(R.layout.cell_choose_semester,viewGroup,false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        final View view = viewHolder.cellView;
        ((TextView) view.findViewById(R.id.yearLabel)).setText(Semester.yearString(i+1));
        final HashMap<Button,Semester> buttonSemesterHashMap = new HashMap<>();
        buttonSemesterHashMap.put((Button)view.findViewById(R.id.button1),new Semester(i+1, Semester.Season.Fall));
        buttonSemesterHashMap.put((Button)view.findViewById(R.id.button2),new Semester(i+1, Semester.Season.IAP));
        buttonSemesterHashMap.put((Button)view.findViewById(R.id.button3),new Semester(i+1, Semester.Season.Spring));
        buttonSemesterHashMap.put((Button)view.findViewById(R.id.button4),new Semester(i+1, Semester.Season.Summer));
        for(final Map.Entry<Button,Semester> buttonSemesterEntry: buttonSemesterHashMap.entrySet()){
            final Button button = buttonSemesterEntry.getKey();
            final Semester semester = buttonSemesterEntry.getValue();
            if(delegateIsValid() && delegate.get().courseInSemester(buttonSemesterEntry.getValue())){
                button.setEnabled(false);
                button.setAlpha(0.5f);
                button.setText("Added");
            }else {
                if(delegateIsValid() && !delegate.get().courseOfferedInSemester(semester))
                    button.setAlpha(0.5f);
                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (delegate != null && delegate.get() != null) {
                            delegate.get().selectSemester(semester);
                        }
                    }
                });
            }
        }
    }

    private boolean delegateIsValid(){
        return delegate != null && delegate.get()!=null;
    }

    @Override
    public int getItemCount() {
        return numYears;
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    static class ViewHolder extends RecyclerView.ViewHolder{
        View cellView;
        ViewHolder(View v){
            super(v);
            this.cellView = v;
        }
    }
}
