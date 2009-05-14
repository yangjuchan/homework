import java.util.*;
import java.io.*;

/* �ɺ��� OP ���̺��� ��� Ŭ���� */

public class Node{
	
	/* �ɺ����̺��� �ɺ���, ����, ���� ��� 
	 * OP���̺��� �ɺ��� Ÿ�� �����ڵ� ���� */
	 
	private String symbol = null; // ���̺� �̸�
	private String line = null; // �ɺ��� �����̼� ī����
	private String opcode = null; // opcode
	private String type = null; // opcode �� ��
	private String code = null; // opcode �� �����ڵ�
	private String csname = null; // es���̺��� �����
	private String name = null; // es���̺��� ��ȣ�̸�
	private String addr = null; // es���̺��� �ּ�
	private String length = null; // es���̺��� ����
	private Node next = null; // ���� ��� ��ũ
	
// --------------------------------------------------------------------------- //	
	
	/* �ɺ����̺� ������ */
	
	public Node(String symbol, String line, Node next){
		this.symbol = symbol;
		this.line = line;
		this.next = next;
	}
	
// --------------------------------------------------------------------------- //	
	
	/* OP���̺� ������ */
	
	public Node(String opcode, String type, String code, Node next){
		this.opcode = opcode;
		this.type = type;
		this.code = code;
	}
	
// --------------------------------------------------------------------------- //	

    /* ES���̺� ������ */
	
	public Node(String csname, String name, String addr, String length, Node next){
		this.csname = csname;
		this.name = name;
		this.addr = addr;
		this.length = length;
	}
	
// --------------------------------------------------------------------------- //	
	
	/* ������ ���� ��ȯ�ϴ� �޼���� */
	
	public String getSymbol(){
		return symbol;
	}
		
	public String getLine(){
		return line;
	}
	
	public String getOpcode(){
		return opcode;
	}
		
	public String getType(){
		return type;
	}
	
	public String getCode(){
		return code;
	}
	
	public String getAddr(){
		return addr;
	}
	
	public String getLength(){
		return length;
	}
	
	public String getCsname(){
		return csname;
	}
	
	public String getName(){
		return name;
	}
	
	public Node getNext(){
		return this.next;
	}
			
	public void setNext(Node node){
		this.next = node;
	}

}