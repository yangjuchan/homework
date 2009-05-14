import java.util.*;
import java.io.*;


/* 해쉬테이블 클래스를 정의 한다. 
 * 심볼테이블의 put 과 search 메서드
 * OP테이블의 Put 메서드 구현 */

public class HashTable{
	
	/* 멤버 변수 설정 */
	
	private Node table[]; // 해쉬테이블 내의 원소
	private Vector SaveNode; // 해쉬테이블 내의 원소들과 링크되는 노드 저장
	//private int NodeCount; // 노드 수
	
	private final int HASHSIZE = 30; // 해쉬 테이블 크기
	
// --------------------------------------------------------------------------- //	
	
	/* 해쉬 테이블 생성자 */
	
	public HashTable(){
		table = new Node[this.HASHSIZE];
		SaveNode = new Vector();
	}
	
// --------------------------------------------------------------------------- //	
	
	/* 해쉬 코드를 얻는 메서드로서
	 * 각 문자의 아스키 값을 더하고
	 * 해쉬 테이블의 크리고 나눈 나머지 값을
	 * 해쉬 코드로 사용 */
	 
	public int getHashCode(String str){
		
		int total = 0; // 문자를 아스키코드로 변환하여 합을 저장하는 변수
		byte[] imsi = null; // 문자를 아스키코드값으로 변환 저장 하는 배열
		
		try{
			imsi = str.getBytes("ASCII");
		}catch(UnsupportedEncodingException ee){}
		
		for(int i=0 ; i<str.length() ; i++){
			total += imsi[i];
		}
		
		return (total%HASHSIZE);
	}
	
// --------------------------------------------------------------------------- //	
	
	/* 해쉬테이블에 원소를 추가 하는 메서드 
	 * 심볼테이블용
	 * 인자값으로 레이블과 로케이션 카운터를 받는다.*/
	
	public void put(String symbol, String line){
		
		Node newNode = new Node(symbol,line, null); // 새로운 노드 생성
		int hashCode = getHashCode(symbol); // 심볼을 해쉬 테이블로 변환하여 저장
		
		// 해쉬 코드가 겹칠경우 처리 부분
		if(table[hashCode] != null){
			SaveNode.addElement(newNode); // 벡터 클래스에 노드 추가
			Node originalNode = table[hashCode]; // 해쉬 테이블내의 노드를 가져옴
			
			if(originalNode.getNext() != null){ // 노드가 또다른 노드를 가지고 있으면
				Node LinkedNode = originalNode.getNext();
				originalNode.setNext(newNode);
				newNode.setNext(LinkedNode);
			}
			else{ // 노드가 새로운 노드를 가지고 있지 않으면 바로 노드 연결
				originalNode.setNext(newNode);
			}
		}
		// 겹치지 않으면 그냥 저장
		else{
			table[hashCode] = newNode;
		}
	}
	
// --------------------------------------------------------------------------- //	
			
	/* 심볼테이블을 검색하여 로케이션 값을 반환하는 메서드 */
	
	public String search(String symbol){
		
		int hashCode = getHashCode(symbol); // 해쉬 코드를 가져옴
		
		// 테이블에 없을경우 리턴
		if(table[hashCode] == null){ 
			return null;
		}
		
		Node n = table[hashCode]; // 테이블내의 노드 가져옴
		
		// 가져온 노드의 심볼값을 비교 하여 리턴
		if(n.getSymbol().equals(symbol)){
			return n.getLine();
		}
		
		// 심볼값과 심볼이 틀릴경우 즉 해쉬코드가 겹쳐서 링크드 리스트로 연결된경우
		else{
			
			if(n.getNext() != null){ // 다음 노드가 있을경우
				
				// 같은 심볼이 있을때까지 계속 링크드 리스트를 탐색
				for(Node e = n.getNext(); e != null ; e = e.getNext()){
					if(e.getSymbol().equals(symbol)){
						return e.getLine();
					}
				}
				return null; // 다 찾아도 없으면 리턴
			}
			// 다음 노드가 없을 경우도 리턴
			else{
				return null;
			}
		}
	}
	
// --------------------------------------------------------------------------- //	
	
	/* 해쉬테이블에 원소를 추가 하는 메서드
	 * OP테이블용
	 * 인자로 OPCODE, 형, 연산코드가 들어 간다. */
	 
	public void put(String opcode, String type, String code){
		
		Node newNode = new Node(opcode, type, code, null); // 새로운 노드 생성
		int hashCode = getHashCode(opcode); // 심볼을 해쉬 테이블로 변환하여 저장
		
		// 해쉬 코드가 겹칠경우 처리 부분
		if(table[hashCode] != null){
			
			SaveNode.addElement(newNode); // 벡터 클래스에 노드 추가
			Node originalNode = table[hashCode]; // 해쉬 테이블내의 노드를 가져옴
			
			if(originalNode.getNext() != null){ // 노드가 또다른 노드를 가지고 있으면
				Node LinkedNode = originalNode.getNext();
				originalNode.setNext(newNode);
				newNode.setNext(LinkedNode);
			}
			else{ // 노드가 새로운 노드를 가지고 있지 않으면 바로 노드 연결
				originalNode.setNext(newNode);
			}
		}
		else{ // 겹치지 않으면 그냥 저장
			table[hashCode] = newNode;
		}
	}
	
// --------------------------------------------------------------------------- //	
	
	/* OP테이블을 검색해서 몇 형식인지를 반환 하는 메서드 */
	
	public String getType(String symbol){
		
		int hashCode = getHashCode(symbol); // 해쉬 코드를 가져옴
		
		// 테이블에 없을경우 리턴
		if(table[hashCode] == null){
			return null;
		}
		
		Node n = table[hashCode]; // 테이블내의 노드 가져옴
		
		// 가져온 노드의 심볼값을 비교 하여 리턴
		if(n.getOpcode().equals(symbol)){
			return n.getType();
		}
		
		// 심볼값과 심볼이 틀릴경우 즉 해쉬코드가 겹쳐서 링크드 리스트로 연결된경우
		else{
			
			// 다음 노드가 있을경우
			if(n.getNext() != null){
				
				// 같은 심볼이 있을때까지 계속 링크드 리스트를 탐색
				for(Node e = n.getNext(); e != null ; e = e.getNext()){
					if(e.getOpcode().equals(symbol)){
						return e.getType();
					}
				}
				return null; // 다 찾아도 없으면 리턴
			}
			else{
				return null; // 다음 노드가 없을 경우도 리턴
			}
		}
	}
	
// --------------------------------------------------------------------------- //	
	
	/* OP테이블을 검색해서 연산코드를  반환 하는 메서드 */
	
	public String getCode(String symbol){
		
		int hashCode = getHashCode(symbol); // 해쉬 코드를 가져옴
		
		// 테이블에 없을경우 리턴
		if(table[hashCode] == null){
			return null;
		}
		
		Node n = table[hashCode]; // 테이블내의 노드 가져옴
		
		// 가져온 노드의 심볼값을 비교 하여 리턴
		if(n.getOpcode().equals(symbol)){
			return n.getCode();
		}
		
		// 심볼값과 심볼이 틀릴경우 즉 해쉬코드가 겹쳐서 링크드 리스트로 연결된경우
		else{
			
			// 다음 노드가 있을경우
			if(n.getNext() != null){
				
				// 같은 심볼이 있을때까지 계속 링크드 리스트를 탐색
				for(Node e = n.getNext(); e != null ; e = e.getNext()){
					if(e.getOpcode().equals(symbol)){
						return e.getCode();
					}
				}
				return null; // 다 찾아도 없으면 리턴
			}
			else{ // 다음 노드가 없을 경우도 리턴
				return null; 
			}
		}
	}
	
// --------------------------------------------------------------------------- //	

    /* 해쉬테이블에 원소를 추가 하는 메서드
	 * ES테이블용
	 * 인자로 제어섹션 이름, 기호이름, 주소, 길이 가 들어간다. */
	 
	public void put(String csname, String name, String addr, String length){
		
		Node newNode = new Node(csname, name, addr, length, null); // 새로운 노드 생성
		
		int hashCode=0;
		if(csname == null){
			hashCode = getHashCode(name); // 심볼을 해쉬 테이블로 변환하여 저장
		}
		else{
			hashCode = getHashCode(csname);
		}
		
		// 해쉬 코드가 겹칠경우 처리 부분
		if(table[hashCode] != null){
			
			SaveNode.addElement(newNode); // 벡터 클래스에 노드 추가
			Node originalNode = table[hashCode]; // 해쉬 테이블내의 노드를 가져옴
			
			if(originalNode.getNext() != null){ // 노드가 또다른 노드를 가지고 있으면
				Node LinkedNode = originalNode.getNext();
				originalNode.setNext(newNode);
				newNode.setNext(LinkedNode);
			}
			else{ // 노드가 새로운 노드를 가지고 있지 않으면 바로 노드 연결
				originalNode.setNext(newNode);
			}
		}
		else{ // 겹치지 않으면 그냥 저장
			table[hashCode] = newNode;
		}
	}
	
// --------------------------------------------------------------------------- //	

    /* ES테이블에서 주소를 반환하는 메서드 
     * 인자로 제어섹션이름이나 기호이름을 받는다. */
	
	public String getAddr(String name){
		
		int hashCode = getHashCode(name); // 해쉬 코드를 가져옴
		
		// 테이블에 없을경우 리턴
		if(table[hashCode] == null){ 
			return null;
		}
		
		Node n = table[hashCode]; // 테이블내의 노드 가져옴
		
		// 가져온 노드의 심볼값을 비교 하여 리턴
		if((n.getCsname().equals(name))||(n.getName().equals(name))){
			return n.getAddr();
		}
		
		// 심볼값과 심볼이 틀릴경우 즉 해쉬코드가 겹쳐서 링크드 리스트로 연결된경우
		else{
			
			if(n.getNext() != null){ // 다음 노드가 있을경우
				
				// 같은 심볼이 있을때까지 계속 링크드 리스트를 탐색
				for(Node e = n.getNext(); e != null ; e = e.getNext()){
					if((e.getCsname().equals(name)||e.getName().equals(name))){
						return e.getAddr();
					}
				}
				return null; // 다 찾아도 없으면 리턴
			}
			// 다음 노드가 없을 경우도 리턴
			else{
				return null;
			}
		}
	}
	
// --------------------------------------------------------------------------- //	
		
	/* 심볼테이블내 모든 원소를 보여주는 메서드  */
	
	public void showSymTable() {
		
		for( int i = 0; i < HASHSIZE ; i++ ) {
			
			//버컷이 비어있으면 메시지 출력하고 다음 주소로
			if( table[i] == null ) {
				System.out.println("Bucket " + (i+1) + "=> null");
				continue;
			}
			
			Node n = table[i]; 	//해쉬 테이블의 내용을 가지고 노드 생성
		
			//노드 내용 프린트
			System.out.print("Bucket " + (i+1) + "=> <" + " 레이블 : "+ 
			n.getSymbol() + " 로케이션 카운터 : " + n.getLine()+" >");
			
			if( n.getNext() != null ) {
				for( Node e = n.getNext(); e != null; e = e.getNext() ) {
					//링크된 노드 프린트
					System.out.print("<"+ " 레이블 : "+ n.getSymbol() + 
					" 로케이션 카운터 : " + n.getLine()+" >");
				}
			}
			System.out.println();
		}
	}

// --------------------------------------------------------------------------- //	
		
	/* OP테이블내 모든 원소를 보여주는 메서드  */
	
	public void showOpTable() {
		
		for( int i = 0; i < HASHSIZE ; i++ ) {
			
			//버컷이 비어있으면 메시지 출력하고 다음 주소로
			if( table[i] == null ) {
				System.out.println("Bucket " + (i+1) + "=> null");
				continue;
			}
			
			Node n = table[i]; 	//해쉬 테이블의 내용을 가지고 노드 생성
		
			//노드 내용 프린트
			System.out.print("Bucket " + (i+1) + "=> <" + " OPCODE : "+ 
			n.getOpcode() + " 형식 : " + n.getType()+ " 연산코드 : "+n.getCode()+" >");
			
			if( n.getNext() != null ) {
				for( Node e = n.getNext(); e != null; e = e.getNext() ) {
					//링크된 노드 프린트
					System.out.print("<"+ " OPCODE : "+ n.getOpcode() + 
					" 형식 : " + n.getType()+ " 연산코드 : "+n.getCode()+" >");
				}
			}
			System.out.println();
		}
	}

// --------------------------------------------------------------------------- //	

    /* ES테이블내 모든 원소를 보여주는 메서드  */
	
	public void showEsTable() {
		
		for( int i = 0; i < HASHSIZE ; i++ ) {
			
			//버컷이 비어있으면 메시지 출력하고 다음 주소로
			if( table[i] == null ) {
				System.out.println("Bucket " + (i+1) + "=> null");
				continue;
			}
			
			Node n = table[i]; 	//해쉬 테이블의 내용을 가지고 노드 생성
		
			//노드 내용 프린트
			System.out.print("Bucket " + (i+1) + "=> <" + " 제어섹션 : "+ 
			n.getCsname() + " 기호이름 : " + n.getName()+ " 주소 : "+n.getAddr()+
			" 길이 : "+n.getLength()+" >");
			
			if( n.getNext() != null ) {
				for( Node e = n.getNext(); e != null; e = e.getNext() ) {
					//링크된 노드 프린트
					System.out.print("<"+ " 제어섹션 : "+ n.getCsname() + 
					" 기호이름 : " + n.getName()+ " 주소 : "+n.getAddr()+
					" 길이 : "+n.getLength()+" >");
				}
			}
			System.out.println();
		}
	}

// --------------------------------------------------------------------------- //	

	/* 테이블내 모든 원소를 삭제하는 매서드 */
        public void removeAll(){
            for( int i = 0; i < HASHSIZE ; i++ ) {
			
		if( table[i] != null ) {
	            table[i] = null;
                }
            }   
	}
}		