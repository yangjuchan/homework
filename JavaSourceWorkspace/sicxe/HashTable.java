import java.util.*;
import java.io.*;


/* �ؽ����̺� Ŭ������ ���� �Ѵ�. 
 * �ɺ����̺��� put �� search �޼���
 * OP���̺��� Put �޼��� ���� */

public class HashTable{
	
	/* ��� ���� ���� */
	
	private Node table[]; // �ؽ����̺� ���� ����
	private Vector SaveNode; // �ؽ����̺� ���� ���ҵ�� ��ũ�Ǵ� ��� ����
	//private int NodeCount; // ��� ��
	
	private final int HASHSIZE = 30; // �ؽ� ���̺� ũ��
	
// --------------------------------------------------------------------------- //	
	
	/* �ؽ� ���̺� ������ */
	
	public HashTable(){
		table = new Node[this.HASHSIZE];
		SaveNode = new Vector();
	}
	
// --------------------------------------------------------------------------- //	
	
	/* �ؽ� �ڵ带 ��� �޼���μ�
	 * �� ������ �ƽ�Ű ���� ���ϰ�
	 * �ؽ� ���̺��� ũ���� ���� ������ ����
	 * �ؽ� �ڵ�� ��� */
	 
	public int getHashCode(String str){
		
		int total = 0; // ���ڸ� �ƽ�Ű�ڵ�� ��ȯ�Ͽ� ���� �����ϴ� ����
		byte[] imsi = null; // ���ڸ� �ƽ�Ű�ڵ尪���� ��ȯ ���� �ϴ� �迭
		
		try{
			imsi = str.getBytes("ASCII");
		}catch(UnsupportedEncodingException ee){}
		
		for(int i=0 ; i<str.length() ; i++){
			total += imsi[i];
		}
		
		return (total%HASHSIZE);
	}
	
// --------------------------------------------------------------------------- //	
	
	/* �ؽ����̺� ���Ҹ� �߰� �ϴ� �޼��� 
	 * �ɺ����̺��
	 * ���ڰ����� ���̺�� �����̼� ī���͸� �޴´�.*/
	
	public void put(String symbol, String line){
		
		Node newNode = new Node(symbol,line, null); // ���ο� ��� ����
		int hashCode = getHashCode(symbol); // �ɺ��� �ؽ� ���̺�� ��ȯ�Ͽ� ����
		
		// �ؽ� �ڵ尡 ��ĥ��� ó�� �κ�
		if(table[hashCode] != null){
			SaveNode.addElement(newNode); // ���� Ŭ������ ��� �߰�
			Node originalNode = table[hashCode]; // �ؽ� ���̺��� ��带 ������
			
			if(originalNode.getNext() != null){ // ��尡 �Ǵٸ� ��带 ������ ������
				Node LinkedNode = originalNode.getNext();
				originalNode.setNext(newNode);
				newNode.setNext(LinkedNode);
			}
			else{ // ��尡 ���ο� ��带 ������ ���� ������ �ٷ� ��� ����
				originalNode.setNext(newNode);
			}
		}
		// ��ġ�� ������ �׳� ����
		else{
			table[hashCode] = newNode;
		}
	}
	
// --------------------------------------------------------------------------- //	
			
	/* �ɺ����̺��� �˻��Ͽ� �����̼� ���� ��ȯ�ϴ� �޼��� */
	
	public String search(String symbol){
		
		int hashCode = getHashCode(symbol); // �ؽ� �ڵ带 ������
		
		// ���̺� ������� ����
		if(table[hashCode] == null){ 
			return null;
		}
		
		Node n = table[hashCode]; // ���̺��� ��� ������
		
		// ������ ����� �ɺ����� �� �Ͽ� ����
		if(n.getSymbol().equals(symbol)){
			return n.getLine();
		}
		
		// �ɺ����� �ɺ��� Ʋ����� �� �ؽ��ڵ尡 ���ļ� ��ũ�� ����Ʈ�� ����Ȱ��
		else{
			
			if(n.getNext() != null){ // ���� ��尡 �������
				
				// ���� �ɺ��� ���������� ��� ��ũ�� ����Ʈ�� Ž��
				for(Node e = n.getNext(); e != null ; e = e.getNext()){
					if(e.getSymbol().equals(symbol)){
						return e.getLine();
					}
				}
				return null; // �� ã�Ƶ� ������ ����
			}
			// ���� ��尡 ���� ��쵵 ����
			else{
				return null;
			}
		}
	}
	
// --------------------------------------------------------------------------- //	
	
	/* �ؽ����̺� ���Ҹ� �߰� �ϴ� �޼���
	 * OP���̺��
	 * ���ڷ� OPCODE, ��, �����ڵ尡 ��� ����. */
	 
	public void put(String opcode, String type, String code){
		
		Node newNode = new Node(opcode, type, code, null); // ���ο� ��� ����
		int hashCode = getHashCode(opcode); // �ɺ��� �ؽ� ���̺�� ��ȯ�Ͽ� ����
		
		// �ؽ� �ڵ尡 ��ĥ��� ó�� �κ�
		if(table[hashCode] != null){
			
			SaveNode.addElement(newNode); // ���� Ŭ������ ��� �߰�
			Node originalNode = table[hashCode]; // �ؽ� ���̺��� ��带 ������
			
			if(originalNode.getNext() != null){ // ��尡 �Ǵٸ� ��带 ������ ������
				Node LinkedNode = originalNode.getNext();
				originalNode.setNext(newNode);
				newNode.setNext(LinkedNode);
			}
			else{ // ��尡 ���ο� ��带 ������ ���� ������ �ٷ� ��� ����
				originalNode.setNext(newNode);
			}
		}
		else{ // ��ġ�� ������ �׳� ����
			table[hashCode] = newNode;
		}
	}
	
// --------------------------------------------------------------------------- //	
	
	/* OP���̺��� �˻��ؼ� �� ���������� ��ȯ �ϴ� �޼��� */
	
	public String getType(String symbol){
		
		int hashCode = getHashCode(symbol); // �ؽ� �ڵ带 ������
		
		// ���̺� ������� ����
		if(table[hashCode] == null){
			return null;
		}
		
		Node n = table[hashCode]; // ���̺��� ��� ������
		
		// ������ ����� �ɺ����� �� �Ͽ� ����
		if(n.getOpcode().equals(symbol)){
			return n.getType();
		}
		
		// �ɺ����� �ɺ��� Ʋ����� �� �ؽ��ڵ尡 ���ļ� ��ũ�� ����Ʈ�� ����Ȱ��
		else{
			
			// ���� ��尡 �������
			if(n.getNext() != null){
				
				// ���� �ɺ��� ���������� ��� ��ũ�� ����Ʈ�� Ž��
				for(Node e = n.getNext(); e != null ; e = e.getNext()){
					if(e.getOpcode().equals(symbol)){
						return e.getType();
					}
				}
				return null; // �� ã�Ƶ� ������ ����
			}
			else{
				return null; // ���� ��尡 ���� ��쵵 ����
			}
		}
	}
	
// --------------------------------------------------------------------------- //	
	
	/* OP���̺��� �˻��ؼ� �����ڵ带  ��ȯ �ϴ� �޼��� */
	
	public String getCode(String symbol){
		
		int hashCode = getHashCode(symbol); // �ؽ� �ڵ带 ������
		
		// ���̺� ������� ����
		if(table[hashCode] == null){
			return null;
		}
		
		Node n = table[hashCode]; // ���̺��� ��� ������
		
		// ������ ����� �ɺ����� �� �Ͽ� ����
		if(n.getOpcode().equals(symbol)){
			return n.getCode();
		}
		
		// �ɺ����� �ɺ��� Ʋ����� �� �ؽ��ڵ尡 ���ļ� ��ũ�� ����Ʈ�� ����Ȱ��
		else{
			
			// ���� ��尡 �������
			if(n.getNext() != null){
				
				// ���� �ɺ��� ���������� ��� ��ũ�� ����Ʈ�� Ž��
				for(Node e = n.getNext(); e != null ; e = e.getNext()){
					if(e.getOpcode().equals(symbol)){
						return e.getCode();
					}
				}
				return null; // �� ã�Ƶ� ������ ����
			}
			else{ // ���� ��尡 ���� ��쵵 ����
				return null; 
			}
		}
	}
	
// --------------------------------------------------------------------------- //	

    /* �ؽ����̺� ���Ҹ� �߰� �ϴ� �޼���
	 * ES���̺��
	 * ���ڷ� ����� �̸�, ��ȣ�̸�, �ּ�, ���� �� ����. */
	 
	public void put(String csname, String name, String addr, String length){
		
		Node newNode = new Node(csname, name, addr, length, null); // ���ο� ��� ����
		
		int hashCode=0;
		if(csname == null){
			hashCode = getHashCode(name); // �ɺ��� �ؽ� ���̺�� ��ȯ�Ͽ� ����
		}
		else{
			hashCode = getHashCode(csname);
		}
		
		// �ؽ� �ڵ尡 ��ĥ��� ó�� �κ�
		if(table[hashCode] != null){
			
			SaveNode.addElement(newNode); // ���� Ŭ������ ��� �߰�
			Node originalNode = table[hashCode]; // �ؽ� ���̺��� ��带 ������
			
			if(originalNode.getNext() != null){ // ��尡 �Ǵٸ� ��带 ������ ������
				Node LinkedNode = originalNode.getNext();
				originalNode.setNext(newNode);
				newNode.setNext(LinkedNode);
			}
			else{ // ��尡 ���ο� ��带 ������ ���� ������ �ٷ� ��� ����
				originalNode.setNext(newNode);
			}
		}
		else{ // ��ġ�� ������ �׳� ����
			table[hashCode] = newNode;
		}
	}
	
// --------------------------------------------------------------------------- //	

    /* ES���̺��� �ּҸ� ��ȯ�ϴ� �޼��� 
     * ���ڷ� ������̸��̳� ��ȣ�̸��� �޴´�. */
	
	public String getAddr(String name){
		
		int hashCode = getHashCode(name); // �ؽ� �ڵ带 ������
		
		// ���̺� ������� ����
		if(table[hashCode] == null){ 
			return null;
		}
		
		Node n = table[hashCode]; // ���̺��� ��� ������
		
		// ������ ����� �ɺ����� �� �Ͽ� ����
		if((n.getCsname().equals(name))||(n.getName().equals(name))){
			return n.getAddr();
		}
		
		// �ɺ����� �ɺ��� Ʋ����� �� �ؽ��ڵ尡 ���ļ� ��ũ�� ����Ʈ�� ����Ȱ��
		else{
			
			if(n.getNext() != null){ // ���� ��尡 �������
				
				// ���� �ɺ��� ���������� ��� ��ũ�� ����Ʈ�� Ž��
				for(Node e = n.getNext(); e != null ; e = e.getNext()){
					if((e.getCsname().equals(name)||e.getName().equals(name))){
						return e.getAddr();
					}
				}
				return null; // �� ã�Ƶ� ������ ����
			}
			// ���� ��尡 ���� ��쵵 ����
			else{
				return null;
			}
		}
	}
	
// --------------------------------------------------------------------------- //	
		
	/* �ɺ����̺� ��� ���Ҹ� �����ִ� �޼���  */
	
	public void showSymTable() {
		
		for( int i = 0; i < HASHSIZE ; i++ ) {
			
			//������ ��������� �޽��� ����ϰ� ���� �ּҷ�
			if( table[i] == null ) {
				System.out.println("Bucket " + (i+1) + "=> null");
				continue;
			}
			
			Node n = table[i]; 	//�ؽ� ���̺��� ������ ������ ��� ����
		
			//��� ���� ����Ʈ
			System.out.print("Bucket " + (i+1) + "=> <" + " ���̺� : "+ 
			n.getSymbol() + " �����̼� ī���� : " + n.getLine()+" >");
			
			if( n.getNext() != null ) {
				for( Node e = n.getNext(); e != null; e = e.getNext() ) {
					//��ũ�� ��� ����Ʈ
					System.out.print("<"+ " ���̺� : "+ n.getSymbol() + 
					" �����̼� ī���� : " + n.getLine()+" >");
				}
			}
			System.out.println();
		}
	}

// --------------------------------------------------------------------------- //	
		
	/* OP���̺� ��� ���Ҹ� �����ִ� �޼���  */
	
	public void showOpTable() {
		
		for( int i = 0; i < HASHSIZE ; i++ ) {
			
			//������ ��������� �޽��� ����ϰ� ���� �ּҷ�
			if( table[i] == null ) {
				System.out.println("Bucket " + (i+1) + "=> null");
				continue;
			}
			
			Node n = table[i]; 	//�ؽ� ���̺��� ������ ������ ��� ����
		
			//��� ���� ����Ʈ
			System.out.print("Bucket " + (i+1) + "=> <" + " OPCODE : "+ 
			n.getOpcode() + " ���� : " + n.getType()+ " �����ڵ� : "+n.getCode()+" >");
			
			if( n.getNext() != null ) {
				for( Node e = n.getNext(); e != null; e = e.getNext() ) {
					//��ũ�� ��� ����Ʈ
					System.out.print("<"+ " OPCODE : "+ n.getOpcode() + 
					" ���� : " + n.getType()+ " �����ڵ� : "+n.getCode()+" >");
				}
			}
			System.out.println();
		}
	}

// --------------------------------------------------------------------------- //	

    /* ES���̺� ��� ���Ҹ� �����ִ� �޼���  */
	
	public void showEsTable() {
		
		for( int i = 0; i < HASHSIZE ; i++ ) {
			
			//������ ��������� �޽��� ����ϰ� ���� �ּҷ�
			if( table[i] == null ) {
				System.out.println("Bucket " + (i+1) + "=> null");
				continue;
			}
			
			Node n = table[i]; 	//�ؽ� ���̺��� ������ ������ ��� ����
		
			//��� ���� ����Ʈ
			System.out.print("Bucket " + (i+1) + "=> <" + " ����� : "+ 
			n.getCsname() + " ��ȣ�̸� : " + n.getName()+ " �ּ� : "+n.getAddr()+
			" ���� : "+n.getLength()+" >");
			
			if( n.getNext() != null ) {
				for( Node e = n.getNext(); e != null; e = e.getNext() ) {
					//��ũ�� ��� ����Ʈ
					System.out.print("<"+ " ����� : "+ n.getCsname() + 
					" ��ȣ�̸� : " + n.getName()+ " �ּ� : "+n.getAddr()+
					" ���� : "+n.getLength()+" >");
				}
			}
			System.out.println();
		}
	}

// --------------------------------------------------------------------------- //	

	/* ���̺� ��� ���Ҹ� �����ϴ� �ż��� */
        public void removeAll(){
            for( int i = 0; i < HASHSIZE ; i++ ) {
			
		if( table[i] != null ) {
	            table[i] = null;
                }
            }   
	}
}		