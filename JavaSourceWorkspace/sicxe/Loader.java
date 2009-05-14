import java.io.*;
import java.util.*;

public class Loader{
	
	/* ��� ���� ���� */
	
	private int progAddr = 0; // ���α׷� ���� �ּ� (�ü���� ���� ����)
	private int csAddr = 0; // ����� ���� �ּ�
	private int csLth = 0; // ����� ����
	private int execAddr = 0; // ���� �ּ�
	
	private final int PROGADDR = 4096; // �޸� ���� �ּ� ���
	
	public HashTable ESTAB; // �ܺ� ��ȣ ���̺�
	
	private BufferedReader inObj = null; // �Է� ���� *.obj
		
	String imsi[] = new String[1500]; // �޸� ��� ����

// --------------------------------------------------------------------------- //

    /* ������ */
    		
	public Loader(){
		ESTAB = new HashTable();
	}
	
// --------------------------------------------------------------------------- //    

    /* �н� 1�� ���� ���ڷ� ������ �޴´� */
	
	public void pass1(String file){ 
		
		String line; // �Է� ���� ����
		String clLabel=null; // ����� �̸�
		String label=null; // ��ȣ �̸�
		
		int indiAddr; // ��ȣ�� �ּ�
		
		progAddr = PROGADDR;
		csAddr = progAddr; // ����� ���� �ּҸ� ���α׷� ���� �ּҷ� ����
		
		try{
			inObj = new BufferedReader(new FileReader(file));
						
			line = inObj.readLine();
						
			while(line != null){ // �Է��� ���� �ƴҶ� ���� �ݺ�
				
				if(line.substring(0,1).equals("H")){ // ��� ���� �϶� ó��
				
					clLabel = line.substring(1,7).trim(); // ������̸� �ֱ�
					csLth = Integer.parseInt(line.substring(14,19),16); // ����� ����
																		
					if(ESTAB.getAddr(clLabel) != null){ // ����� �̸��� ������
					
						System.out.println("�ܺα�ȣ���̺� �ߺ� ����");
						return;
					}
					else{ // ���� ���� �̸��� ������
						ESTAB.put(clLabel,null,Integer.toHexString(csAddr),Integer.toHexString(csLth));
					}
				}
				
				while(!line.substring(0,1).equals("E")){ // ���� ���ڵ� ������ ���� �ݺ�
				
					line = inObj.readLine();
					
					if(line.substring(0,1).equals("D")){ // ���� ���ڵ带 ������ ó�� �κ�
						
						int count=1; // ���п� ī����
						for(int i=1 ; i<=(line.length()-1)/12 ; i++){
							
							label = line.substring(count,count+6).trim(); // ��ȣ�̸�
							indiAddr=Integer.parseInt(line.substring(count+6,count+12),16); // ��ȣ�� �ּ�
							
							count+=12; // ���п� ī����
							if(ESTAB.getAddr(label) != null){
								System.out.println("�ܺα�ȣ���̺� �ߺ� ����");
								return;
							}
							else{
								ESTAB.put(null,label,Integer.toHexString(csAddr+indiAddr),null);
							}
						}
					}
				}
				csAddr+=csLth; // ���� ����� ���� �ּҸ� ���� ���� ���� ����
				line = inObj.readLine(); // ���� ���� �б�
			}
		}catch(FileNotFoundException e){ // ���� ���� üũ
    		System.out.println(e.toString());
    	}catch(IOException e){ // ����� ���� üũ
    		System.out.println(e.toString());
    	}
    }
    
// --------------------------------------------------------------------------- //    

    /* �н� 2 �� ���� ���ڷ� ������Ʈ ������ �޴´�. */
    
    public void pass2(String file){
    	
    	csAddr = PROGADDR;
        execAddr = PROGADDR;
    	String label, line;
    	StringBuffer objectData = new StringBuffer(5000);
    	
    	int imsiLength=0;
		int imsiLength2=0;
		int count=0;
    	
    	int startProg=0;
    	
    	try{
    		inObj = new BufferedReader(new FileReader(file));
						
			line = inObj.readLine();
						
			while(line != null){ // �Է��� ���� �ƴҶ� ���� �ݺ�
			    
			    if(line.substring(0,1).equals("H")){ // ��� ���� �϶� ó��
				
					csLth = Integer.parseInt(line.substring(14,19),16); // ����� ����
					startProg = Integer.parseInt(line.substring(8,13),16); // ���α׷� ���� �ּ�
				}
				
				while(!line.substring(0,1).equals("E")){ // ���� ���ڵ� ������ ���� �ݺ�
				
					line = inObj.readLine(); // ���� ���� �б�
					if(line.substring(0,1).equals("T")){ // �ؽ�Ʈ ���ڵ带 ������ 
					    
					    int c=0;
					    if(count !=0){
					    	c = Integer.parseInt(line.substring(1,7),16)-(imsiLength+imsiLength2);
					    }
					    count++;
					    imsiLength = Integer.parseInt(line.substring(1,7),16);
						imsiLength2 = Integer.parseInt(line.substring(7,9),16);
						for(int i=0 ; i<c*2 ; i++){
							objectData.append("x");
						}
						objectData.append(line.substring(9).trim());
						
						
					}
					else if(line.substring(0,1).equals("M")){ // ���� ���ڵ带 ������
						
						label = line.substring(9); // ��ȣ�� ������ ���̺��� ����
						
						int a = (Integer.parseInt(line.substring(1,7),16)-startProg)*2;
						int b = (Integer.parseInt(line.substring(1,7),16)-startProg)*2+6;
						
						if(ESTAB.getAddr(label.substring(1).trim()) != null){ // ���̺��� ������
							if(label.substring(0,1).equals("+")){
								
								String imsi = objectData.toString();
								
								imsi = imsi.substring(a, b);
								
								int i = Integer.parseInt(imsi,16)+Integer.parseInt(ESTAB.getAddr(label.substring(1)),16);
																
								objectData.replace(a, b, Integer.toHexString(i).toUpperCase());
							}
							else{
								String imsi = objectData.toString();
								
								imsi = imsi.substring(a, b);
								
								int i = Integer.parseInt(imsi,16)-Integer.parseInt(ESTAB.getAddr(label.substring(1)),16);
																
								objectData.replace(a, b, Integer.toHexString(i).toUpperCase());
							}
						}
						else{
							System.out.println("���̺� ���� ����");
							return;
						}
					} // �������ڵ� ��
				} // ���� ���ڵ尡 �ƴϸ�  ��
				
				if(line.substring(0,1).equals("E")){
					
					if(line.length() > 2){
						execAddr = csAddr + Integer.parseInt(line.substring(1).trim());
					}
				}
				csAddr+=csLth; // ���� ���� ���� ���� �ּҸ� ���� ���� ���� ���� ���̸� ����
				line = inObj.readLine(); // ���� ���� �б�
			}
		}catch(FileNotFoundException e){ // ���� ���� üũ
    		System.out.println(e.toString());
    	}catch(IOException e){ // ����� ���� üũ
    		System.out.println(e.toString());
    	}
    	// �������̽��� ��� �κ�
    	
    	String inputData = objectData.toString();
    	
    	if(inputData.length()%8 !=0){ // 8�� ����� �ƴ� ��� ó��
    		for(int i=0 ; i<inputData.length()%8 ; i++){
    			inputData+="xx";
    		}
    	}
    	
    	int a=0; // �ӽ� ����
    	
    	for(int i=0 ; i<inputData.length()/8 ; i++){ // �޸� �������(8)�� ������.
    		a=i*8;
    		imsi[i] = inputData.substring(a,a+8);
    	}
    	
        int counta=0; // �ӽú���
        for(int i=0; i<300 ; i++){ // ���̺� ��� �κ�
        	for(int j=1; j<5 ; j++){
        		if(counta == imsi.length-1){
        			break;
        		}
        		Sicxe.table.setValueAt(imsi[counta],i,j);
        		counta++;
        	}
        }	
    } // pass2 ��
}

// --------------------------------------------------------------------------- //    
    
    