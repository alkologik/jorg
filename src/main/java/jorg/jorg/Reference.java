package jorg.jorg;

public class Reference {

    private String id;
    private boolean declared;

    public Reference(String id) {
        this(id , false);
    }

    public Reference(String id, boolean declared) {
        this.id = id;
        this.declared = declared;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isDeclared() {
        return declared;
    }

    public void setDeclared(boolean declared) {
        this.declared = declared;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Reference && id.equals(((Reference) obj).id);
    }

    @Override
    public String toString() {
        return "#" + id;
    }
}
