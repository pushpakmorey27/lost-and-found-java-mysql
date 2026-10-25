// A class is a plan for an object. One Student object holds one student's details.
// CONCEPT: Encapsulation keeps the values private and reads them through get methods.
public class Student {
    // private means other classes cannot directly change these fields.
    // final means the value is assigned once, when this object is created.
    private final int studentId;
    private final String studentName;
    private final String contact;

    // A constructor runs when we write: new Student(id, name, contact).
    public Student(int studentId, String studentName, String contact) {
        // this.studentId is the field; studentId is the constructor's input.
        this.studentId = studentId;
        this.studentName = studentName;
        this.contact = contact;
    }

    // A getter returns one private value without allowing direct changes to it.
    public int getStudentId() {
        return studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getContact() {
        return contact;
    }
}
