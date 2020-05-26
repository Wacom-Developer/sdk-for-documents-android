package com.wacom.baxter.sdk.example.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GroupItem extends BaseItem {

    List<BaseItem> mChildren = new ArrayList<BaseItem>();
    Object mBaxterObject;
    public GroupItem(String name) {
        super(name);
    }

    public void addChild(BaseItem item) {
        mChildren.add(item);
    }

    public List<BaseItem> getChildren() {
        return mChildren;
    }

    public void setBaxterObject(Object baxterObject) {
        mBaxterObject = baxterObject;
    }

    public Object getBaxterObject() {
        return mBaxterObject;
    }

}
