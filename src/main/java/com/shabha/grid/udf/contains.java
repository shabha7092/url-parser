package com.shabha.grid.udf;


import java.io.IOException;
import org.apache.pig.FilterFunc;
import org.apache.pig.backend.executionengine.ExecException;
import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.schema.Schema;



public class contains extends FilterFunc {

    @Override
    public Boolean exec(Tuple input) throws IOException {
        try {
            if (input == null || input.size() < 2) {
                throw new IOException("Not enough arguments to " + this.getClass().getName() + ": got " + input.size() + ", expected at least 2");
            }
            if (input.get(0) == null || input.get(1) == null) {
                return false;
            }
            String value = (String) input.get(0);
            String subValue = (String) input.get(1);
            return value.contains(subValue);
        } catch (ExecException e) {
            throw new IOException(e);
        }
    }


@Override
public Schema outputSchema(Schema input) {
    return new Schema(new Schema.FieldSchema(null, DataType.BOOLEAN));
}

}
