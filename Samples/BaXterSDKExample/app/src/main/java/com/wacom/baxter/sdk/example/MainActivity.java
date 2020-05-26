package com.wacom.baxter.sdk.example;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfDocument;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.wacom.baxter.Document;
import com.wacom.baxter.Field;
import com.wacom.baxter.Page;
import com.wacom.baxter.PenData;
import com.wacom.baxter.document.AuthoringTool;
import com.wacom.baxter.document.ClientApp;
import com.wacom.baxter.document.ClientDevice;
import com.wacom.baxter.document.DocumentCompletionTime;
import com.wacom.baxter.document.PageCompletionOrder;
import com.wacom.baxter.document.PageID;
import com.wacom.baxter.document.PageIDList;
import com.wacom.baxter.document.SmartPad;
import com.wacom.baxter.document.SmartPadCharacteristics;
import com.wacom.baxter.exception.InvalidDocumentException;
import com.wacom.baxter.field.FieldCompletionTime;
import com.wacom.baxter.field.FieldData;
import com.wacom.baxter.field.FieldKeyName;
import com.wacom.baxter.field.FieldLocation;
import com.wacom.baxter.field.FieldName;
import com.wacom.baxter.field.FieldReason;
import com.wacom.baxter.field.FieldType;
import com.wacom.baxter.field.FieldUUID;
import com.wacom.baxter.page.FieldIDList;
import com.wacom.baxter.pendata.Point;
import com.wacom.baxter.pendata.Stroke;
import com.wacom.baxter.sdk.example.data.BaseItem;
import com.wacom.baxter.sdk.example.data.GroupItem;
import com.wacom.baxter.sdk.example.data.Item;
import com.wacom.baxter.sdk.example.views.LevelBeamView;
import com.wacom.bootstrap.lib.InvalidLicenseException;
import com.wacom.bootstrap.lib.LicenseBootstrap;
import com.wacom.bootstrap.lib.LicenseTokenException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import pl.openrnd.multilevellistview.ItemInfo;
import pl.openrnd.multilevellistview.MultiLevelListAdapter;
import pl.openrnd.multilevellistview.MultiLevelListView;

public class MainActivity extends AppCompatActivity {

    private static final String CREATE_TEST_DOCUMENT = Environment.getExternalStorageDirectory()+"/baxterTestDocument.pdf";
    private static final String LICENSE = "COPY THE LICENCE STRING HERE";

    private MultiLevelListView mListView;
    private ListAdapter mListAdapter;
    private Document baxterDocument;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // this is only an example of use of the Wacom Signature SDK, so we don't bother about
        // permissions, this should be handle properly in real apps
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        View openBtn = findViewById(R.id.btn_open_file);
        openBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFile();
            }
        });

        View saveBtn = findViewById(R.id.btn_save_file);
        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveXmp(true);
            }
        });

        View createBtn = findViewById(R.id.btn_new_document);
        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    createDocument();
                } catch (IOException e) {
                    showMessage(e.toString());
                }
            }
        });

        mListView = (MultiLevelListView) findViewById(R.id.listView);
        mListAdapter = new ListAdapter();
        mListView.setAdapter(mListAdapter);

        try {
            LicenseBootstrap.initLicense(this.getApplicationContext(), LICENSE.getBytes());
        } catch (LicenseTokenException e) {
            showMessage(e.toString());
        }

    }

    private void selectFile() {
        File mPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
        FileDialog fileDialog = new FileDialog(this, mPath, ".pdf");
        fileDialog.addFileListener(new FileDialog.FileSelectedListener() {
            public void fileSelected(File file) {
                Log.d(getClass().getName(), "selected file " + file.toString());
                loadBaXterData(file);
            }
        });

        fileDialog.showDialog();
    }

    private void saveXmp(boolean showMessage) {
        try {
            if (baxterDocument != null) {
                baxterDocument.save(this);
                if (showMessage) {
                    showMessage("Data saved properly");
                }
            }
        } catch (InvalidLicenseException e) {
            showMessage(e.toString());
        }
    }

    private void loadBaXterData(File file) {
        try {
            baxterDocument = Document.loadDocument(file.getAbsolutePath());
            GroupItem root = new GroupItem(file.getAbsolutePath());
            root.setBaxterObject(baxterDocument);

            //Document Level Definitions
            GroupItem authoringToolNode = new GroupItem("Authoring Tool");
            Item authoringToolName = new Item("Name", baxterDocument.getAuthoringTool().getToolname());
            authoringToolName.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    baxterDocument.getAuthoringTool().setToolname(value);
                }
            });

            Item authoringToolVersion = new Item("Version", baxterDocument.getAuthoringTool().getVersion());
            authoringToolVersion.setListener(new Item.ItemListener() {

                @Override
                public void editValue(String value) {
                    baxterDocument.getAuthoringTool().setVersion(value);
                }
            });

            authoringToolNode.addChild(authoringToolName);
            authoringToolNode.addChild(authoringToolVersion);
            root.addChild(authoringToolNode);

            List<Integer> completionOrderList = baxterDocument.getPageCompletionOrder().getPageCompletionOrder();
            Item completionOrderItem = new Item("Completion order", completionOrderList.toString().replace("[", "").replace("]", "").trim());
            completionOrderItem.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    String[] values = value.split(",");
                    List<Integer> order = new ArrayList<Integer>();
                    for (String pageNum : values) {
                        order.add(Integer.parseInt(pageNum.trim()));
                    }
                    baxterDocument.setPageCompletionOrder(new PageCompletionOrder(order));
                }
            });
            root.addChild(completionOrderItem);

            final DocumentCompletionTime documentCompletionTime = baxterDocument.getDocumentCompletionTime();
            Item completionTimeItem = new Item("Document completion time", documentCompletionTime.getTime());
            completionTimeItem.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    documentCompletionTime.setTime(value);
                }
            });
            root.addChild(completionTimeItem);

            final SmartPad smartPad = baxterDocument.getSmartPad();
            GroupItem smartPadGroup = new GroupItem("SmartPad");
            Item smartPadID = new Item("ID", smartPad.getId());
            smartPadID.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    smartPad.setId(value);
                }
            });
            smartPadGroup.addChild(smartPadID);
            Item deviceName = new Item("Device name", smartPad.getDeviceName());
            deviceName.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    smartPad.setDeviceName(value);
                }
            });
            smartPadGroup.addChild(deviceName);
            root.addChild(smartPadGroup);

            final SmartPadCharacteristics smartPadCharacteristics = baxterDocument.getSmartPadCharacteristics();
            GroupItem characteristics = new GroupItem("SmartPadCharacteristics");
            Item unit = new Item("Unit", smartPadCharacteristics.getUnit());
            unit.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    smartPadCharacteristics.setUnit(value);
                }
            });
            characteristics.addChild(unit);
            Item pointsPerUnit = new Item("Points per Unit", smartPadCharacteristics.getPointsPerUnit());
            pointsPerUnit.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    smartPadCharacteristics.setPointsPerUnit(value);
                }
            });
            characteristics.addChild(pointsPerUnit);
            Item width = new Item("Width", smartPadCharacteristics.getWidth());
            width.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    smartPadCharacteristics.setWidth(value);
                }
            });
            characteristics.addChild(width);
            Item height = new Item("Height", smartPadCharacteristics.getHeight());
            height.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    smartPadCharacteristics.setHeight(value);
                }
            });
            characteristics.addChild(height);
            root.addChild(characteristics);

            final ClientApp clientApp = baxterDocument.getClientApp();
            GroupItem clientAppGroup = new GroupItem("Client app");
            Item version = new Item("Version", clientApp.getVersion());
            version.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    clientApp.setVersion(value);
                }
            });
            clientAppGroup.addChild(version);
            Item os = new Item("OS", clientApp.getOs());
            os.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    clientApp.setOs(value);
                }
            });
            clientAppGroup.addChild(os);
            Item appName = new Item("Application name", clientApp.getApplicationName());
            appName.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    clientApp.setApplicationName(value);
                }
            });
            clientAppGroup.addChild(appName);
            root.addChild(clientAppGroup);

            final ClientDevice clientDevice = baxterDocument.getClientDevice();
            GroupItem clientDeviceGroup = new GroupItem("Client device");
            Item id = new Item("ID", clientDevice.getId());
            id.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    clientDevice.setId(value);
                }
            });
            clientDeviceGroup.addChild(id);
            Item deviceClass = new Item("Device class", clientDevice.getDeviceClass());
            deviceClass.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    clientDevice.setDeviceClass(value);
                }
            });
            clientDeviceGroup.addChild(deviceClass);
            Item deviceNameItem = new Item("Device name", clientDevice.getDeviceName());
            deviceNameItem.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    clientDevice.setDeviceName(value);
                }
            });
            clientDeviceGroup.addChild(deviceNameItem);
            root.addChild(clientDeviceGroup);

            GroupItem pages = new GroupItem("Pages");
            List<Page> pageList = baxterDocument.getPages();
            fillPages(pages, pageList);
            root.addChild(pages);

            List<BaseItem> data = new ArrayList<BaseItem>();
            data.add(root);
            mListAdapter.setDataItems(data);

        } catch (InvalidDocumentException | InvalidLicenseException e) {
            showMessage(e.toString());
        }

    }

    private void fillPages(GroupItem parent, List<Page> pageList) {
        if (pageList != null) {
            int index = 1;
            for (Page page : pageList) {
                GroupItem pageGroup = new GroupItem("Page "+index++);
                pageGroup.setBaxterObject(page);
                fillPage(pageGroup, page);
                parent.addChild(pageGroup);
            }
        }
    }

    private void fillPage(GroupItem parent, Page page) {
        final PageID pageID = page.getPageID();
        GroupItem pageIDNode = new GroupItem("PageID");
        Item pdfPage = new Item("pdfPage", pageID.getPdfPage());
        pdfPage.setListener(new Item.ItemListener() {

            @Override
            public void editValue(String value) {
                pageID.setPdfPage(value);
            }
        });

        Item uuid = new Item("uuid", pageID.getUuid());
        uuid.setListener(new Item.ItemListener() {

            @Override
            public void editValue(String value) {
                pageID.setUuid(value);
            }
        });

        pageIDNode.addChild(pdfPage);
        pageIDNode.addChild(uuid);
        parent.addChild(pageIDNode);

        FieldIDList fieldIDList = page.getFieldIDList();
        GroupItem fieldIDListNode = new GroupItem("FieldIDList");
        List<String> fieldNames = fieldIDList.getFieldIDList();
        if (!fieldNames.isEmpty()) {
            for (String fieldName : fieldNames) {
                Item fieldNameItem = new Item("", fieldName);
                fieldIDListNode.addChild(fieldNameItem);
            }
        }
        parent.addChild(fieldIDListNode);

        List<Field> fieldList = page.getFieldList();
        if (!fieldList.isEmpty()) {
            GroupItem fieldListNode = new GroupItem("Fields");
            for (Field field : fieldList) {
                String fieldName = "Field "+ field.getFieldUUID().getFieldID();
                GroupItem fieldNode = new GroupItem(fieldName);
                fieldNode.setBaxterObject(field);
                fillField(fieldNode, field);
                fieldListNode.addChild(fieldNode);
            }
            parent.addChild(fieldListNode);
        }

        fillPenData(parent, page.getPenData());

    }

    private void fillField(GroupItem parent, final Field field) {
        final FieldUUID fieldID = field.getFieldUUID();
        GroupItem fieldIDNode = new GroupItem("Field ID");
        Item fieldIDItem = new Item("Pdf ID", fieldID.getPdfID());
        fieldIDItem.setListener(new Item.ItemListener() {
            @Override
            public void editValue(String value) {
                fieldID.setPdfID(value);
            }
        });

        Item fieldUUIDItem = new Item("UUID", fieldID.getFieldID());
        fieldUUIDItem.setListener(new Item.ItemListener() {
            @Override
            public void editValue(String value) {
                fieldID.setFieldID(value);
            }
        });

        fieldIDNode.addChild(fieldIDItem);
        fieldIDNode.addChild(fieldUUIDItem);
        parent.addChild(fieldIDNode);

        GroupItem locationNode = new GroupItem("Location");
        FieldLocation fieldLocation = field.getFieldLocation();
        Item locationX = new Item("location X", fieldLocation.getX());
        locationX.setListener(new Item.ItemListener() {
            @Override
            public void editValue(String value) {
                field.getFieldLocation().setX(value);
            }
        });

        Item locationY = new Item("location Y", fieldLocation.getY());
        locationY.setListener(new Item.ItemListener() {
            @Override
            public void editValue(String value) {
                field.getFieldLocation().setY(value);
            }
        });

        Item locationW = new Item("Width", fieldLocation.getWidth());
        locationW.setListener(new Item.ItemListener() {
            @Override
            public void editValue(String value) {
                field.getFieldLocation().setWidth(value);
            }
        });

        Item locationH = new Item("Height", fieldLocation.getHeight());
        locationH.setListener(new Item.ItemListener() {
            @Override
            public void editValue(String value) {
                field.getFieldLocation().setHeight(value);
            }
        });

        locationNode.addChild(locationX);
        locationNode.addChild(locationY);
        locationNode.addChild(locationW);
        locationNode.addChild(locationH);
        parent.addChild(locationNode);

        Item typeItem = new Item("Type", field.getFieldType().getType().name());
        typeItem.setListener(new Item.ItemListener() {
            @Override
            public void editValue(String value) {
                FieldType.Type type = FieldType.Type.UNKNOWN;
                switch (value.toUpperCase()) {
                    case "BOOLEAN":
                        type = FieldType.Type.BOOLEAN;
                        break;
                    case "FREEHAND":
                        type = FieldType.Type.FREEHAND;
                        break;
                    case "NUMBER":
                        type = FieldType.Type.NUMBER;
                        break;
                    case "SIGNATURE":
                        type = FieldType.Type.SIGNATURE;
                        break;
                    case "TEXT":
                        type = FieldType.Type.TEXT;
                        break;
                }
                field.getFieldType().setType(type);
            }
        });
        parent.addChild(typeItem);

        if (field.getFieldType().getType() == FieldType.Type.SIGNATURE) {
            Item encryptedItem = new Item("Encrypted", Boolean.toString(field.getFieldEncrypted().isEncrypted()));
            encryptedItem.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    field.getFieldEncrypted().setEncrypted(Boolean.parseBoolean(value));
                }
            });
            parent.addChild(encryptedItem);

            final FieldKeyName fieldKeyName = field.getFieldKeyName();
            Item keyNameItem = new Item("Key name", fieldKeyName.getKeyName());
            keyNameItem.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    fieldKeyName.setKeyName(value);
                }
            });
            parent.addChild(keyNameItem);

            final FieldName fieldName = field.getFieldName();
            Item nameItem = new Item("Name", fieldName.getName());
            nameItem.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    fieldName.setName(value);
                }
            });
            parent.addChild(nameItem);

            final FieldReason fieldReason = field.getFieldReason();
            Item reasonItem = new Item("Reason", fieldReason.getReason());
            reasonItem.setListener(new Item.ItemListener() {
                @Override
                public void editValue(String value) {
                    fieldReason.setReason(value);
                }
            });
            parent.addChild(reasonItem);
        }

        Item tagItem = new Item("Tag", field.getFieldTag().getTag());
        tagItem.setListener(new Item.ItemListener() {
            @Override
            public void editValue(String value) {
                field.getFieldTag().setTag(value);
            }
        });
        parent.addChild(tagItem);

        Item requiredItem = new Item("Required", Boolean.toString(field.getFieldRequired().isRequired()));
        requiredItem.setListener(new Item.ItemListener() {
            @Override
            public void editValue(String value) {
                field.getFieldRequired().setRequired(Boolean.parseBoolean(value));
            }
        });
        parent.addChild(requiredItem);

        final FieldCompletionTime fieldCompletionTime = field.getFieldCompletionTime();
        Item completionTimeItem = new Item("Completion time", fieldCompletionTime.getTime());
        completionTimeItem.setListener(new Item.ItemListener() {
            @Override
            public void editValue(String value) {
                fieldCompletionTime.setTime(value);
            }
        });
        parent.addChild(completionTimeItem);

        final FieldData fieldData = field.getFieldData();
        Item dataItem = new Item("Data", fieldData.getData());
        dataItem.setListener(new Item.ItemListener() {
            @Override
            public void editValue(String value) {
                fieldData.setData(value);
            }
        });
        parent.addChild(dataItem);

        fillPenData(parent, field.getPenData());
    }

    private void fillPenData(GroupItem parent, PenData penData) {
        List<Stroke> strokes = penData.getStrokes();
        if (!strokes.isEmpty()) {
            GroupItem penDataGroup = new GroupItem("PenData");
            for (Stroke stroke : strokes) {
                GroupItem strokeGroup = new GroupItem("Stroke");
                for (final Point point : stroke.getPoints()) {
                    GroupItem pointGroup = new GroupItem("Point");
                    Item x = new Item("x", point.getX());
                    x.setListener(new Item.ItemListener() {
                        @Override
                        public void editValue(String value) {
                            point.setX(value);
                        }
                    });

                    Item y = new Item("y", point.getY());
                    y.setListener(new Item.ItemListener() {
                        @Override
                        public void editValue(String value) {
                            point.setY(value);
                        }
                    });

                    Item w = new Item("w", point.getW());
                    w.setListener(new Item.ItemListener() {
                        @Override
                        public void editValue(String value) {
                            point.setW(value);
                        }
                    });

                    Item c = new Item("color", point.getInkColor());
                    c.setListener(new Item.ItemListener() {
                        @Override
                        public void editValue(String value) {
                            point.setInkColor(value);
                        }
                    });

                    pointGroup.addChild(x);
                    pointGroup.addChild(y);
                    pointGroup.addChild(w);
                    pointGroup.addChild(c);
                    strokeGroup.addChild(pointGroup);
                }
                penDataGroup.addChild(strokeGroup);
            }
            parent.addChild(penDataGroup);
        }

    }


    private class ListAdapter extends MultiLevelListAdapter {

        private class ViewHolder {
            TextView nameView;
            TextView valueView;
            ImageView arrowView;
            LevelBeamView levelBeamView;
            TextView toXMPView;
            TextView editView;
        }

        @Override
        public List<?> getSubObjects(Object object) {
            if (object instanceof GroupItem) {
                return ((GroupItem) object).getChildren();
            } else {
                return null;
            }
        }

        @Override
        public boolean isExpandable(Object object) {
            return object instanceof GroupItem;
        }

        @Override
        public View getViewForObject(final Object object, View convertView, ItemInfo itemInfo) {
            final ViewHolder viewHolder;
            if (convertView == null) {
                viewHolder = new ViewHolder();
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.data_item, null);
                viewHolder.valueView = (TextView) convertView.findViewById(R.id.dataItemInfo);
                viewHolder.nameView = (TextView) convertView.findViewById(R.id.dataItemName);
                viewHolder.arrowView = (ImageView) convertView.findViewById(R.id.dataItemArrow);
                viewHolder.levelBeamView = (LevelBeamView) convertView.findViewById(R.id.dataItemLevelBeam);
                viewHolder.toXMPView = (TextView) convertView.findViewById(R.id.xmp_btn);
                viewHolder.editView = (TextView) convertView.findViewById(R.id.edit_btn);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.nameView.setText(((BaseItem) object).getName());

            if (object instanceof Item) {
                viewHolder.valueView.setVisibility(View.VISIBLE);
                viewHolder.valueView.setText(((Item) object).getValue());

                viewHolder.editView.setVisibility(View.VISIBLE);
                viewHolder.editView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Item.ItemListener listener = ((Item) object).getListener();
                        if (listener != null) {
                            editValueDialog((Item)object, listener);
                        }
                    }
                });
            } else {
                viewHolder.valueView.setVisibility(View.GONE);
                viewHolder.editView.setVisibility(View.GONE);
            }

            if (itemInfo.isExpandable()) {
                viewHolder.arrowView.setVisibility(View.VISIBLE);
                viewHolder.arrowView.setImageResource(itemInfo.isExpanded() ?
                        R.drawable.arrow_up : R.drawable.arrow_down);
            } else {
                viewHolder.arrowView.setVisibility(View.GONE);
            }

            if (object instanceof GroupItem) {
                final Object baxterObject = ((GroupItem) object).getBaxterObject();
                if (baxterObject != null) {
                    viewHolder.toXMPView.setVisibility(View.VISIBLE);
                    viewHolder.toXMPView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            generateXmp(baxterObject);
                        }
                    });
                } else {
                    viewHolder.toXMPView.setVisibility(View.GONE);
                }
            } else {
                viewHolder.toXMPView.setVisibility(View.GONE);
            }

            viewHolder.levelBeamView.setLevel(itemInfo.getLevel());

            return convertView;
        }
    }

    private void generateXmp(Object object) {
        try {
            String xmp = null;
            if (object instanceof Document) {
                xmp = ((Document) object).toXMP();
            } else if (object instanceof Page) {
                xmp = ((Page) object).toXMP();
            } else if (object instanceof Field) {
                xmp = ((Field) object).toXMP();
            }

            if (xmp != null) {
                final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                alertDialog.setMessage(xmp);
                alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        alertDialog.dismiss();
                    }
                });
                alertDialog.show();
            }
        } catch (InvalidLicenseException ile) {
            final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setMessage(ile.getMessage());
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    alertDialog.dismiss();
                }
            });
            alertDialog.show();
        }
    }

    private void editValueDialog(final Item item, final Item.ItemListener listener) {
        final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.edit_value_dialog, null);
        final EditText editText = (EditText) dialogView.findViewById(R.id.edit_value);
        editText.setText(item.getValue());
        Button acceptButton = (Button) dialogView.findViewById(R.id.accept_btn);
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                item.setValue(editText.getText().toString());
                listener.editValue(editText.getText().toString());
                mListAdapter.notifyDataSetChanged();
                alertDialog.dismiss();
            }
        });
        alertDialog.setView(dialogView);
        alertDialog.show();
    }

    private void showMessage(String message) {
        final AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setMessage(message);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private void createDocument() throws IOException {
        baxterDocument = new Document(CREATE_TEST_DOCUMENT);
        baxterDocument.setAuthoringTool(new AuthoringTool("BaXter Sample Code", "Test"));

        List<Integer> pageOrderList = new ArrayList<Integer>();
        pageOrderList.add(1);
        baxterDocument.setPageCompletionOrder(new PageCompletionOrder(pageOrderList));

        File templateFile = new File(CREATE_TEST_DOCUMENT);
        if (templateFile.exists()) {
            templateFile.delete();
        }

        copyTemplateFromAssets(templateFile);

        Page page1 = new Page();
        page1.setPageID(new PageID("1", "123456789012", PageID.BarcodeSymbology.EAN));
        page1.addBarcode(this, templateFile.getAbsolutePath());
        baxterDocument.addPage(page1);

        // save the file and reload it.
        saveXmp(false);
        loadBaXterData(new File(CREATE_TEST_DOCUMENT));
    }

    private void copyTemplateFromAssets(File dst) throws IOException {
        try (InputStream in = getAssets().open("template.pdf")) {
            try (OutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }
}
