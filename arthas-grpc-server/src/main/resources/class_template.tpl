${package}

import java.io.Serializable;
<!-- $BeginBlock imports -->
import ${importBlock};
<!-- $EndBlock imports -->

public class ${className} implements ${codecClassName}<${targetProxyClassName}>, Serializable {
	public static final long serialVersionUID = 1L;
    private ${descriptorClsName} descriptor;

    public byte[] encode(${targetProxyClassName} target) throws IOException {
        CodedOutputStreamCache outputCache = CodedOutputStreamCache.get();
        doWriteTo(target, outputCache.getCodedOutputStream());
        return outputCache.getData();
    }

    public void doWriteTo(${targetProxyClassName} t, CodedOutputStream output)
            throws IOException {
        <!-- $BeginBlock encodeFields -->
        ${dynamicFieldType} ${dynamicFieldName} = null;
        if (!FieldUtil.isNull(${dynamicFieldGetter})) {
            ${dynamicFieldName} = ${dynamicFieldGetter};
            ${encodeWriteFieldValue}
        }
        <!-- $EndBlock encodeFields -->
    }

    public ${targetProxyClassName} decode(byte[] bb) throws IOException {
        CodedInputStream input = CodedInputStream.newInstance(bb, 0, bb.length);
        return readFrom(input);
    }

    public int size(${targetProxyClassName} target) throws IOException {
        int size = 0;
        <!-- $BeginBlock encodeFields -->
        ${dynamicFieldType} ${dynamicFieldName} = null;
        if (!CodedConstant.isNull(${dynamicFieldGetter})) {
            ${dynamicFieldName} = ${dynamicFieldGetter};
            size += ${sizeDynamicString}
        }
        <!-- $EndBlock encodeFields -->
        return size;
    }
 
    public ${targetProxyClassName} readFrom(CodedInputStream input) throws IOException {
        ${targetProxyClassName} ret = new ${targetProxyClassName}();
        
        ${initListMapFields}

        <!-- $BeginBlock enumFields -->
        ${enumInitialize};
        <!-- $EndBlock enumFields -->
        try {
            boolean done = false;
            Codec codec = null;
            while (!done) {
                int tag = input.readTag();
                if (tag == 0) {
                    break;
                }
                <!-- $BeginBlock decodeFields -->
                if (tag == ${decodeOrder}) {
                    ${objectDecodeExpress}
                    ${decodeFieldSetValue}
                    ${objectDecodeExpressSuffix}
                    ${deocdeCheckNull}
                    continue;
                }
                ${objectPackedDecodeExpress}
                <!-- $EndBlock decodeFields -->               
                
                input.skipField(tag);
            }
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            throw e;
        } catch (java.io.IOException e) {
            throw e;
        }

        return ret;       
        
    } 
 
 
     public com.google.protobuf.Descriptors.Descriptor getDescriptor() throws IOException {
        if (this.descriptor != null) {
            return this.descriptor;
        }
        com.google.protobuf.Descriptors.Descriptor descriptor =
                CodedConstant.getDescriptor(${targetProxyClassName}.class);
        return (this.descriptor = descriptor);
    }   
}

    