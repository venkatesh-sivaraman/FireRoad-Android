package com.base12innovations.android.fireroad.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.base12innovations.android.fireroad.utils.DocumentIconView;
import com.base12innovations.android.fireroad.R;
import com.base12innovations.android.fireroad.models.course.ColorManager;
import com.base12innovations.android.fireroad.models.course.Course;
import com.base12innovations.android.fireroad.models.doc.Document;
import com.base12innovations.android.fireroad.models.doc.DocumentManager;
import com.base12innovations.android.fireroad.utils.TaskDispatcher;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentBrowserAdapter extends RecyclerView.Adapter<DocumentBrowserAdapter.ViewHolder> {

    public interface Delegate {
        void documentBrowserSelectedDocument(Document doc);
        void documentBrowserLongPressedRow(int index, View sourceView);
    }

    public WeakReference<Delegate> delegate;

    private DocumentManager documentManager;
    private Context context;

    public DocumentBrowserAdapter(Context context, DocumentManager manager) {
        this.context = context;
        this.documentManager = manager;

    }

    @Override
    public int getItemCount() {
        if (documentManager == null) {
            return 0;
        }
        return documentManager.getItemCount();
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View cellView;

        public ViewHolder(View v) {
            super(v);
            this.cellView = v;
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        final LayoutInflater layoutInflater = LayoutInflater.from(viewGroup.getContext());
        View convertView = layoutInflater.inflate(R.layout.cell_document_browser, viewGroup, false);
        ViewHolder vh = new ViewHolder(convertView);
        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int i) {
        final View view = viewHolder.cellView;
        ((TextView)view.findViewById(R.id.titleLabel)).setText(documentManager.getDocumentName(i));
        ((TextView)view.findViewById(R.id.descriptionLabel)).setText(documentManager.getDocumentModifiedString(i));

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TaskDispatcher.inBackground(new TaskDispatcher.TaskNoReturn() {
                    @Override
                    public void perform() {
                        final Document doc = documentManager.getNonTemporaryDocument(viewHolder.getAdapterPosition());
                        TaskDispatcher.onMain(new TaskDispatcher.TaskNoReturn() {
                            @Override
                            public void perform() {
                                if (delegate.get() != null) {
                                    delegate.get().documentBrowserSelectedDocument(doc);
                                }
                            }
                        });
                    }
                });
            }
        });

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (delegate.get() != null) {
                    delegate.get().documentBrowserLongPressedRow(viewHolder.getAdapterPosition(), viewHolder.cellView);
                    return true;
                }
                return false;
            }
        });

        // Draw color view
        final Document tempDoc = documentManager.getTempDocument(i);
        TaskDispatcher.perform(new TaskDispatcher.Task<List<DocumentIconView.ColorSector>>() {
            @Override
            public List<DocumentIconView.ColorSector> perform() {
                tempDoc.read();
                Map<Integer, DocumentIconView.ColorSector> sectors = new HashMap<>();
                List<Course> allCourses = tempDoc.getAllCourses();
                for (Course course : allCourses) {
                    int color = ColorManager.colorForCourse(course);
                    if (!sectors.containsKey(color))
                        sectors.put(color, new DocumentIconView.ColorSector(color, 1.0f / (float) allCourses.size()));
                    else
                        sectors.get(color).proportion += 1.0f / (float) allCourses.size();
                }

                if (sectors.size() > 0) {
                    List<DocumentIconView.ColorSector> sectorList = new ArrayList<>(sectors.values());
                    Collections.sort(sectorList, new Comparator<DocumentIconView.ColorSector>() {
                        @Override
                        public int compare(DocumentIconView.ColorSector t1, DocumentIconView.ColorSector t2) {
                            return Float.compare(t1.proportion, t2.proportion);
                        }
                    });
                    return sectorList;
                }
                return null;
            }
        }, new TaskDispatcher.CompletionBlock<List<DocumentIconView.ColorSector>>() {
            @Override
            public void completed(List<DocumentIconView.ColorSector> sectors) {
                DocumentIconView iconView = view.findViewById(R.id.documentIcon);
                if (documentManager.getDocumentType().equals(Document.SCHEDULE_DOCUMENT_TYPE))
                    iconView.iconShape = DocumentIconView.IconShape.CIRCLE;
                iconView.setColorSectors(sectors);
                iconView.invalidate();
            }
        });
    }
}
