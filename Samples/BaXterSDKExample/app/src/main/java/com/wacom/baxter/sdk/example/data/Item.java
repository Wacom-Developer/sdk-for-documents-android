/******************************************************************************
 *
 *  2016 (C) Copyright Open-RnD Sp. z o.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 ******************************************************************************/

package com.wacom.baxter.sdk.example.data;

public class Item extends BaseItem {

    public interface ItemListener {
        public void editValue(String value);
    }

    private String mValue;
    private ItemListener mListener;

    public Item(String name, String value) {
        super(name);
        mValue = value;
    }

    public void setValue(String value) {
        mValue = value;
    }

    public String getValue() {
        return mValue;
    }

    public void setListener(ItemListener listener) {
        mListener = listener;
    }

    public ItemListener getListener() {
        return mListener;
    }
}
