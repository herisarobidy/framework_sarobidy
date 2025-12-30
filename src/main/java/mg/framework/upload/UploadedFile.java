package mg.framework.upload;

public class UploadedFile {
    private final String fieldName;
    private final String submittedFileName;
    private final String contentType;
    private final long size;
    private final byte[] bytes;

    public UploadedFile(String fieldName, String submittedFileName, String contentType, long size, byte[] bytes) {
        this.fieldName = fieldName;
        this.submittedFileName = submittedFileName;
        this.contentType = contentType;
        this.size = size;
        this.bytes = bytes;
    }

    public String getFieldName() { return fieldName; }
    public String getSubmittedFileName() { return submittedFileName; }
    public String getContentType() { return contentType; }
    public long getSize() { return size; }
    public byte[] getBytes() { return bytes; }

    public String getExtension() {
        if (submittedFileName == null) return null;
        int idx = submittedFileName.lastIndexOf('.');
        if (idx == -1 || idx == submittedFileName.length() - 1) return null;
        return submittedFileName.substring(idx + 1).toLowerCase();
    }

    public String getExtensionOrGuess() {
        String ext = getExtension();
        if (ext != null) return ext;
        if (contentType == null) return null;
        String ct = contentType.toLowerCase();
        if (ct.contains("jpeg") || ct.contains("jpg")) return "jpg";
        if (ct.contains("png")) return "png";
        if (ct.contains("gif")) return "gif";
        if (ct.contains("pdf")) return "pdf";
        if (ct.contains("msword") || ct.contains("word")) return "doc";
        if (ct.contains("officedocument.wordprocessingml")) return "docx";
        if (ct.contains("excel") || ct.contains("officedocument.spreadsheetml")) return "xls";
        if (ct.contains("zip")) return "zip";
        if (ct.contains("text")) return "txt";
        return null;
    }

    public boolean hasExtension() {
        return getExtension() != null;
    }
}
