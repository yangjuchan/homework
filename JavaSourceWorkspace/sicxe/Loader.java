import java.io.*;
import java.util.*;

public class Loader{
	
	/* 멤버 변수 선언 */
	
	private int progAddr = 0; // 프로그램 시작 주소 (운영체제에 의해 받음)
	private int csAddr = 0; // 제어섹션 시작 주소
	private int csLth = 0; // 제어섹션 길이
	private int execAddr = 0; // 실행 주소
	
	private final int PROGADDR = 4096; // 메모리 시작 주소 상수
	
	public HashTable ESTAB; // 외부 기호 테이블
	
	private BufferedReader inObj = null; // 입력 파일 *.obj
		
	String imsi[] = new String[1500]; // 메모리 블록 단위

// --------------------------------------------------------------------------- //

    /* 생성자 */
    		
	public Loader(){
		ESTAB = new HashTable();
	}
	
// --------------------------------------------------------------------------- //    

    /* 패스 1을 구현 인자로 파일을 받는다 */
	
	public void pass1(String file){ 
		
		String line; // 입력 받은 라인
		String clLabel=null; // 제어섹션 이름
		String label=null; // 기호 이름
		
		int indiAddr; // 기호의 주소
		
		progAddr = PROGADDR;
		csAddr = progAddr; // 제어섹션 지작 주소를 프로그램 시작 주소로 세팅
		
		try{
			inObj = new BufferedReader(new FileReader(file));
						
			line = inObj.readLine();
						
			while(line != null){ // 입력이 널이 아닐때 까지 반복
				
				if(line.substring(0,1).equals("H")){ // 헤더 라인 일때 처리
				
					clLabel = line.substring(1,7).trim(); // 제어섹션이름 넣기
					csLth = Integer.parseInt(line.substring(14,19),16); // 제어섹션 길이
																		
					if(ESTAB.getAddr(clLabel) != null){ // 제어섹션 이름이 있으면
					
						System.out.println("외부기호테이블 중복 에러");
						return;
					}
					else{ // 제어 섹션 이름이 없으면
						ESTAB.put(clLabel,null,Integer.toHexString(csAddr),Integer.toHexString(csLth));
					}
				}
				
				while(!line.substring(0,1).equals("E")){ // 엔드 레코드 만날때 까지 반복
				
					line = inObj.readLine();
					
					if(line.substring(0,1).equals("D")){ // 정의 레코드를 만나면 처리 부분
						
						int count=1; // 구분용 카운터
						for(int i=1 ; i<=(line.length()-1)/12 ; i++){
							
							label = line.substring(count,count+6).trim(); // 기호이름
							indiAddr=Integer.parseInt(line.substring(count+6,count+12),16); // 기호의 주소
							
							count+=12; // 구분용 카운터
							if(ESTAB.getAddr(label) != null){
								System.out.println("외부기호테이블 중복 에러");
								return;
							}
							else{
								ESTAB.put(null,label,Integer.toHexString(csAddr+indiAddr),null);
							}
						}
					}
				}
				csAddr+=csLth; // 다음 제어섹션 시작 주소를 위해 섹션 길이 더함
				line = inObj.readLine(); // 다음 라인 읽기
			}
		}catch(FileNotFoundException e){ // 파일 에러 체크
    		System.out.println(e.toString());
    	}catch(IOException e){ // 입출력 에러 체크
    		System.out.println(e.toString());
    	}
    }
    
// --------------------------------------------------------------------------- //    

    /* 패스 2 를 구현 인자로 오브젝트 파일을 받는다. */
    
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
						
			while(line != null){ // 입력이 널이 아닐때 까지 반복
			    
			    if(line.substring(0,1).equals("H")){ // 헤더 라인 일때 처리
				
					csLth = Integer.parseInt(line.substring(14,19),16); // 제어섹션 길이
					startProg = Integer.parseInt(line.substring(8,13),16); // 프로그램 시작 주소
				}
				
				while(!line.substring(0,1).equals("E")){ // 엔드 레코드 만날때 까지 반복
				
					line = inObj.readLine(); // 다음 라인 읽기
					if(line.substring(0,1).equals("T")){ // 텍스트 레코드를 만나면 
					    
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
					else if(line.substring(0,1).equals("M")){ // 수정 레코드를 만나면
						
						label = line.substring(9); // 기호를 포함한 레이블을 저장
						
						int a = (Integer.parseInt(line.substring(1,7),16)-startProg)*2;
						int b = (Integer.parseInt(line.substring(1,7),16)-startProg)*2+6;
						
						if(ESTAB.getAddr(label.substring(1).trim()) != null){ // 레이블이 있으면
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
							System.out.println("레이블 없어 에러");
							return;
						}
					} // 수정레코드 끝
				} // 엔드 레코드가 아니면  끝
				
				if(line.substring(0,1).equals("E")){
					
					if(line.length() > 2){
						execAddr = csAddr + Integer.parseInt(line.substring(1).trim());
					}
				}
				csAddr+=csLth; // 다음 제어 섹션 시작 주소를 위해 현재 제어 섹션 길이를 더함
				line = inObj.readLine(); // 다음 라인 읽기
			}
		}catch(FileNotFoundException e){ // 파일 에러 체크
    		System.out.println(e.toString());
    	}catch(IOException e){ // 입출력 에러 체크
    		System.out.println(e.toString());
    	}
    	// 인터페이스에 출력 부분
    	
    	String inputData = objectData.toString();
    	
    	if(inputData.length()%8 !=0){ // 8의 배수가 아닐 경우 처리
    		for(int i=0 ; i<inputData.length()%8 ; i++){
    			inputData+="xx";
    		}
    	}
    	
    	int a=0; // 임시 변수
    	
    	for(int i=0 ; i<inputData.length()/8 ; i++){ // 메모리 블락단위(8)로 나눈다.
    		a=i*8;
    		imsi[i] = inputData.substring(a,a+8);
    	}
    	
        int counta=0; // 임시변수
        for(int i=0; i<300 ; i++){ // 테이블에 출력 부분
        	for(int j=1; j<5 ; j++){
        		if(counta == imsi.length-1){
        			break;
        		}
        		Sicxe.table.setValueAt(imsi[counta],i,j);
        		counta++;
        	}
        }	
    } // pass2 끝
}

// --------------------------------------------------------------------------- //    
    
    