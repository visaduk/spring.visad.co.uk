package uk.co.visad.model.presence;

public class FieldUpdateMessage {

    private String table;   // "travelers" or "dependents"
    private Long id;
    private String field;
    private String value;
    private String updatedBy; // set by server from Principal

    public FieldUpdateMessage() {}

    public String getTable() { return table; }
    public void setTable(String table) { this.table = table; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
}
