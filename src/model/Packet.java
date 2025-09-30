package model;

import java.io.Serializable;

import lombok.Data;

@Data
public class Packet implements Serializable {
    private String type; // "LOGIN", "MESSAGE",...
    private Object data;
}