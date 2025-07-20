package org.example.AlredyExist;

public class AlredyExistException extends RuntimeException{

    public AlredyExistException (String fieldName) {
        super(fieldName + " alredy exist");
    }
    
}
