package cn.ctkqiang.huaxiahongke.adpters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import cn.ctkqiang.huaxiahongke.R;

public class MenuAdapter extends BaseAdapter {

    private Context context;
    private List<String> menuList;

    public MenuAdapter(Context context, List<String> menuList) {
        this.context = context;
        this.menuList = menuList;
    }

    @Override
    public int getCount() {
        return menuList.size();
    }

    @Override
    public Object getItem(int position) {
        return menuList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_grid, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.textView);
        textView.setText(menuList.get(position));

        return convertView;
    }
}
