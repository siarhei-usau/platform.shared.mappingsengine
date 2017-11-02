package com.ebsco.entarch.mappings.streamsets;

import com.streamsets.pipeline.api.Field;
import com.streamsets.pipeline.api.Record;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.ebsco.platform.shared.mappingsengine.core.StringUtils.*;

@UtilityClass
public class RecordUtils {

    public Field write(Record record, String path, String value, Map<String, String> fieldAttributes, boolean append) {
        ensureFieldPathIsSettable(record, path);
        Field field = Field.create(value);
        fieldAttributes.forEach(field::setAttribute);
        record.set(path, field);
        return field;
    }

    public Field write(Record record, String path, String value) {
        return write(record, path, value, new HashMap<>(), false);
    }



    private void ensureFieldPathIsSettable(Record record, String path) {
        String currentPath = "";
        Field currentField = Optional.ofNullable(record.get("/")).filter(it -> it.getType() == Field.Type.MAP)
                .orElseThrow(() -> new IllegalStateException("No root map element!"));

        List<String> parts = Arrays.stream(path.split("/"))
                .filter(it -> !it.trim().isEmpty())
                .collect(Collectors.toList());

        for (int idx = 0; idx < parts.size(); ++idx) {
            String step = parts.get(idx);
            boolean isLast = idx == parts.size() - 1;
            String baseName = substringBefore(step, "[", step);
            String arrayIndexString = removeSuffix(substringAfter(step, "[", ""), "]");
            Integer arrayIndex = null;
            if (!arrayIndexString.trim().isEmpty()) {
                arrayIndex = Integer.parseInt(arrayIndexString);
            }
            if (arrayIndex == null) {
                // we have a map key instead of array index, and we only act on that if it is in the middle of a path
                if (!isLast) {
                    // so we are at CURRENT in the middle of a path
                    //    something/other/CURRENT/...
                    //    something/other[0]/CURRENT/...
                    String newFieldName = currentPath + "/" + baseName;
                    currentField = record.get(newFieldName);
                    if (currentField == null) {
                        currentField = Field.create(new HashMap<>());
                        record.set(newFieldName, currentField);
                    }
                    if (currentField.getType() != Field.Type.MAP) {
                        throw new IllegalStateException("Existing field " + newFieldName +
                                " should be of type ELEMENT ans id instead " + currentField.getType());
                    }
                    currentPath = newFieldName;
                }
            } else {
                // we are a list element, and lists can only contain maps for us, so we are one of
                //     something/other/CURRENT[N]/...
                //     something/other/CURRENT[N]
                //     something/other[0]/CURRENT[N]/...
                //     something/other[0]/CURRENT[N]

                String newFieldName = currentPath + "/" + baseName;
                Field newFieldList = record.get(newFieldName);
                if (newFieldList == null) {
                    newFieldList = Field.create(new ArrayList<>());
                    record.set(newFieldName, currentField);
                }
                if (newFieldList.getType() != Field.Type.LIST) {
                    throw new IllegalStateException("Existing field " + newFieldName +
                            " should be of type ARRAYINDEX ans id instead " + currentField.getType());
                }

                // make sure we have enough elements to include this index
                List<Field> contents = newFieldList.getValueAsList();
                while (contents.size() < arrayIndex + 1) {
                    contents.add(Field.create(new HashMap<>()));
                }
                String newIndexFieldName = newFieldName + "[" + arrayIndex + "]";
                currentField = Optional.ofNullable(record.get(newIndexFieldName))
                        .orElseThrow(() -> new IllegalStateException("Missing filed " + newIndexFieldName));
                if (currentField.getType() != Field.Type.MAP) {
                    throw new IllegalStateException("Existing field " + newFieldName +
                            " should be of type ELEMENT ans id instead " + currentField.getType());
                }
                currentPath = newIndexFieldName;
            }
        }
    }
}
