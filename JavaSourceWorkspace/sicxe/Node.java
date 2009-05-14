import java.util.*;
import java.io.*;

/* 심볼과 OP 테이블의 노드 클래스 */

public class Node{
	
	/* 심볼테이블의 심볼과, 라인, 다음 노드 
	 * OP테이블의 심볼과 타입 연산코드 정의 */
	 
	private String symbol = null; // 레이블 이름
	private String line = null; // 심볼의 로케이션 카운터
	private String opcode = null; // opcode
	private String type = null; // opcode 의 형
	private String code = null; // opcode 의 연산코드
	private String csname = null; // es테이블의 제어섹션
	private String name = null; // es테이블의 기호이름
	private String addr = null; // es테이블의 주소
	private String length = null; // es테이블의 길이
	private Node next = null; // 다음 노드 링크
	
// --------------------------------------------------------------------------- //	
	
	/* 심볼테이블 생성자 */
	
	public Node(String symbol, String line, Node next){
		this.symbol = symbol;
		this.line = line;
		this.next = next;
	}
	
// --------------------------------------------------------------------------- //	
	
	/* OP테이블 생성자 */
	
	public Node(String opcode, String type, String code, Node next){
		this.opcode = opcode;
		this.type = type;
		this.code = code;
	}
	
// --------------------------------------------------------------------------- //	

    /* ES테이블 생성자 */
	
	public Node(String csname, String name, String addr, String length, Node next){
		this.csname = csname;
		this.name = name;
		this.addr = addr;
		this.length = length;
	}
	
// --------------------------------------------------------------------------- //	
	
	/* 각각의 값을 반환하는 메서드들 */
	
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