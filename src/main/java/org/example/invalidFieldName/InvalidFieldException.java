package org.example.invalidFieldName;

public class InvalidFieldException extends RuntimeException {
    public InvalidFieldException(String fieldName) {
        super("Invalid " + fieldName);
    }
}
