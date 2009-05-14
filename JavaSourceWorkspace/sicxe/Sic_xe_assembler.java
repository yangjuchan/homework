import java.io.*;
import java.util.*;

public class Sic_xe_assembler {

    /* 각 멤버 변수 설정 */
    
    private int startAddr = 0; // 시작 주소 초기화
    private int progLength = 0; // 프로그램 길이
    
    public HashTable SYMTAB,OPTAB; // 심볼테이블과 OP테이블
    
    private BufferedReader input = null; // 입력(소스) 파일
    private PrintWriter outInter = null; // 중간 파일 출력
    private PrintWriter outList = null; // LIST 파일 출력
    private PrintWriter outObj = null; // OBJ 파일 출력
               
// --------------------------------------------------------------------------- //    
    
    /* 어셈블러 생성자!
     * 파일을 인자로 받는다! */
     
    public Sic_xe_assembler(){
        
        opTable(); // op테이블 생성 
        SYMTAB = new HashTable(); // 심볼테이블 생성
    }

// --------------------------------------------------------------------------- //    
    
    /* 패스 1을 구현
     * 인자로 소스 파일을 받는다~ */
     
    public void pass1(String file){
    	
    	String line; // 파일에서 라인단위로 잘라 받기 위한 변수
    	int lc=0; // 위치 로케이션 카운터 초기화
    	int pc=0; // pc카운터 초기화
    	int count=1; // 중간파일에 들어갈 라인 변수
    	
    	// 첫번째 라인을 읽어 토큰으로 나누어서 각 필드에 저장
    	try{
    		input = new BufferedReader(new FileReader(file));
    		// '이름.int' 라는 중간 파일 출력 부분
    		outInter = new PrintWriter(new BufferedWriter(new FileWriter(file.substring(0,file.indexOf('.'))+".int")));
    		line = input.readLine();
    		
    		
    		// 라인을 분석
    		Tokenizer.parse(line);
    		
    		// 'START' 지시자를 찾음
    		if(Tokenizer.OPCODE.equals("START")){
    			// 오퍼랜드에서 시작주소를 읽어 저장하고 lc 를 세팅함
    			startAddr = Integer.parseInt(Tokenizer.OPERAND, 16);
    			lc = pc = startAddr;
    			
    			    			
    			printInterFile(count++, lc, line);
    			
    			// 다음 라인을 읽고 분석
    			line = input.readLine();
    			Tokenizer.parse(line);
    		}
    		
    		// 'END' 지시자를 만날때까지 실행
    		while(!Tokenizer.OPCODE.equals("END")){
    			
    			if (!Tokenizer.LABEL.equals(".")){ // 주석이 아니라면 실행
    				
    				// OPCODE를 체크
    				// 4형식이 아니면 무조건 3형식으로 인식
    				if(Tokenizer.TYPE_MODE == 'F'){ // 4형식이면
    					lc = pc += 4;
    					lc -= 4;
    				}
    				// @어셈블러 지시자 먼저 체크(널포인터 EXCEPTION 떄문에)@
    				else if(Tokenizer.OPCODE.equals("WORD")){
    				 	lc = pc += 3;
    				 	lc -= 3;
    				}
    				else if(Tokenizer.OPCODE.equals("RESW")){
    				 	lc = pc += 3*(Integer.parseInt(Tokenizer.OPERAND));
    				 	lc -= 3*(Integer.parseInt(Tokenizer.OPERAND));
    				}
    				else if(Tokenizer.OPCODE.equals("RESB")){
    				 	lc = pc += Integer.parseInt(Tokenizer.OPERAND);
    				 	lc -= Integer.parseInt(Tokenizer.OPERAND);
    				}
    				else if(Tokenizer.OPCODE.equals("BYTE")){
    				 	// 'C', 'X'를 구분하여 증가
    				 	if(Tokenizer.BYTE_MODE == 'X'){
    				 		lc = pc += 1;
    				 		lc -= 1;
    				 	}
    				 	else if(Tokenizer.BYTE_MODE == 'C'){
    				 		lc = pc += Tokenizer.OPERAND.length();
    				 		lc -= Tokenizer.OPERAND.length();
    				 	}
    				}
    				else if(Tokenizer.OPCODE.equals("BASE")){
    				}
    				
    				// 1,2,3 형식 구분 
    				else if(OPTAB.getType(Tokenizer.OPCODE).equals("1")){
    				    lc = pc += 1;
    				    lc -= 1;
    				}
    				else if(OPTAB.getType(Tokenizer.OPCODE).equals("2")){
    				    lc = pc += 2;
    				    lc -= 2;
    				}
    			    else if(OPTAB.getType(Tokenizer.OPCODE).equals("3")){
    				   	lc = pc += 3;
    				   	lc -= 3;
    				}
    				// OP테이블에도 없고 어셈블러 지시자도 아니면 에러 표시
    				else {
    				 	System.out.println("오피 코드 읍네 에러다!");
    				 	return;
    				}
    				// 중간파일 출력, 베이스는 출력 안하기위한 코드
    				if (!Tokenizer.OPCODE.equals("BASE")){
    					printInterFile(count++, lc, line);
    				}
    				else{
    					printInterFile(count++, 0, line);
    				}
    				
    				// LABEL을 체크
    				if(Tokenizer.LABEL.length() > 0 ){ // 레이블이 있으면
    				
    				    if(SYMTAB.search(Tokenizer.LABEL) == null){ // 심볼테이블에 없으면
    				        SYMTAB.put(Tokenizer.LABEL,Integer.toHexString(lc));
    				    }
    				    else{ // 심볼테이블에 있으면
    				    	System.out.println("레이블 중복 에러!!!");
    				    	return;
    				    }
    				}
				}
    			else{ // 주석이라면
    				printInterFile(count++, 0, line);
    			}
    			
    			// 다음 라인 읽고 분석
    			line = input.readLine();
    			Tokenizer.parse(line);
    		}
    		
    		printInterFile(count++,0 ,line); // END 용 중간파일 출력
    		progLength = pc - startAddr; // 프로그램 길이를 구함
    		input.close(); // 소스파일 닫기
    		outInter.close(); // 중간파일 닫기
          // --------------------------------------------------------- //
             // 초기화
            lc = pc = 0;
            count=1;
          // -------------------------------------------------------- //
    	}catch(FileNotFoundException e){ // 파일 에러 체크
    		System.out.println(e.toString());
    	}catch(IOException e){ // 입출력 에러 체크
    		System.out.println(e.toString());
    	}
    }
    
// --------------------------------------------------------------------------- //

    /* 패스 2 구현 */
    
    public void pass2(String file){
    	
    	String buffer = ""; 
    	String objCode = "";
    	String line;
    	String interLine;
    	String mName=null;
    	BufferedReader interInput;
    	
    	int base, pc;
    	int objStartAddr = base = pc = startAddr; // 오브젝트파일 시작 주소
    	int objLength = 0; // 오브젝트 파일 길이
    	int opndAddr = 0; // 오퍼랜드 주소
    	int codeAddr = 0; // 코드 주소
    	int[] modifi = new int[10]; // 수정 레코드번지 저장 변수 (10개정도 선언)
    	int count=0; // 수정 레코드용 카운터
    	
    	try{
    		
    		// 각각 입출력 파일객체들을 생성
    		input = new BufferedReader(new FileReader(file));
    		interInput = new BufferedReader(new FileReader(file.substring(0,file.indexOf('.'))+".int"));
    		outList = new PrintWriter(new BufferedWriter(new FileWriter(file.substring(0,file.indexOf('.'))+".lst")));
    		outObj = new PrintWriter(new BufferedWriter(new FileWriter(file.substring(0,file.indexOf('.'))+".obj")));
    		
    		line = input.readLine(); // 소스파일 읽어 들임 (패스 2 처리용)
    		interLine = interInput.readLine(); // 중간파일을 읽어 들임(리스트 출력용)
    		
    		Tokenizer.parse(line); // 소스라인 재 분석(패스 2에서 사용하기 위해)
    		
    		// 'START' 지시어를 찾음
    		if(Tokenizer.OPCODE.equals("START")){
    			
    			// 중간파일 출력
    			printListFile(interLine,"");
    			
    			mName = Tokenizer.LABEL;
    			
    			// 헤더파일 출력
    			outObj.println(("H" + Formatter(Tokenizer.LABEL, 6, 1, 1) + 
    			                Formatter(Integer.toHexString(startAddr), 6, 0, 0) +
    			                Formatter(Integer.toHexString(progLength), 6, 0, 0)).toUpperCase());
    			Sicxe.atObj.append(("H" + Formatter(Tokenizer.LABEL, 6, 1, 1) + 
    			                Formatter(Integer.toHexString(startAddr), 6, 0, 0) +
    			                Formatter(Integer.toHexString(progLength), 6, 0, 0)).toUpperCase());
    			Sicxe.atObj.append("\n");
    			
    			// 다음 라인 읽고 분석
    			line = input.readLine();
    			interLine = interInput.readLine();
    			
    			Tokenizer.parse(line);
    		}
    		
    		// 'END' 지시자를 찾을때까지 실행
    		while(!Tokenizer.OPCODE.equals("END")){
    			
    			if(!Tokenizer.LABEL.equals(".")){ // 주석이 아니면
    			    
    			    if (OPTAB.getCode(Tokenizer.OPCODE) != null){ // OP테이블에 있으면
    			    	
    			    	if (Tokenizer.XE_MODE == 'Y'){ // SIC/XE 라면
    			    		
    			    		if(Tokenizer.TYPE_MODE == 'F'){ // 4형식이면
    			    		 	
    			    		 	if(Tokenizer.ADDR_MODE == 'I'){ // 즉시주소지정방식이면
    			    		
    			    		        if(SYMTAB.search(Tokenizer.OPERAND) != null){ // 오퍼랜드가 레이블이면
    			    			        opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16) | 0x100000;
    			    			        modifi[count] = pc+1;
    			    			        count++;
    			    	            }
    			    		        else{ // 오퍼랜드가 상수이면
    			    			        opndAddr = Integer.parseInt(Tokenizer.OPERAND) | 0x100000;
    			    		        }
    			    		
    			    		        pc += 4;
    			    		        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+1;
    			    		        objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 6, 0, 0);
    			    		        printListFile(interLine,objCode);
    			    		        buffer += objCode;
    			    		        objLength += 4;
    			    	        }
    			    	
    			    	        else if(Tokenizer.ADDR_MODE == 'N'){ // 간접주소지정방식이면
    			    	
    			    		        pc += 4;
    			    		        opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16) | 0x100000;
    			    		        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+2;
    			    		        objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 6, 0, 0);
    			    		        printListFile(interLine,objCode);
    			    		        buffer += objCode;
    			    		        objLength += 4;
     			    	        }
    			    	
    			    	        else{ // 둘다 아니면
    			    	
    			    		        modifi[count] = pc+1;
    			    			    count++;
    			    			    pc += 4;
    			    		        opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16) | 0x100000;
    			    		        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    		        objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 6, 0, 0);
    			    		        printListFile(interLine,objCode);
    			    		        buffer += objCode;
    			    		        objLength += 4;
    			    	        }
    			            } // 4형식이면 끝
    			            
    			            else if(OPTAB.getType(Tokenizer.OPCODE).equals("1")){ // 1형식이면
    			    
    			    	        pc += 1;
    			    	        objCode = OPTAB.getCode(Tokenizer.OPCODE);
    			    	        printListFile(interLine,objCode);
    			    	        buffer += objCode;
    			    	        objLength += 1;
    			            }
    			    
    			            else if(OPTAB.getType(Tokenizer.OPCODE).equals("2")){ // 2형식이면
    			        
    			                pc += 2;
    			                codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16);
    			                StringTokenizer a = new StringTokenizer(Tokenizer.OPERAND,",");
    			        
    			                String first = a.nextToken();
    			                String second = "0";
    			                if (a.countTokens()!=0){second = a.nextToken();}
    			            			        			        
    			                objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+register(first)+register(second);
    			                printListFile(interLine,objCode);
    			    	        buffer += objCode;
    			    	        objLength += 2;
    			            }
    			    
    			            else if(Tokenizer.OPCODE.equals("RSUB")){ // "RSUB"일 경우
    			    	
    			    	        pc += 3;
    			    	        opndAddr = 0;
    			    	        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    	        objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    	        printListFile(interLine,objCode);
    			    	        buffer += objCode;
    			    	        objLength += 3;
    			            }
    			            
    			            else if(OPTAB.getType(Tokenizer.OPCODE).equals("3")){ // 3형식이면
    			        
    			                if(Tokenizer.ADDR_MODE == 'I'){ // 즉시주소지정방식이라면
    			        
    			                    pc += 3;
    			            
    			                    if(SYMTAB.search(Tokenizer.OPERAND) != null){ // 오퍼랜드가 레이블이면
    			            
    			                        opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16);
    			                
    			                            if (opndAddr-pc > 4096 || opndAddr-pc < -4096){ // 베이스 상대 주소지정방식
    			                                opndAddr = (opndAddr-base) | 0x004000;
    			                            }
    			                            else{ // 프로그램 카운터 주소지정방식
    			                	            opndAddr = (opndAddr-pc) | 0x002000;
    			                            }
    			                    }
    			            
    			                    else{
    			            	        opndAddr = Integer.parseInt(Tokenizer.OPERAND,16);
    			                    }
    			                    
    			                    if (opndAddr < 0){ // - 부분 처리
    			                
    			                        String a = Integer.toHexString(opndAddr);
    			                        a = a.substring(a.length()-3);
    			                        opndAddr = Integer.parseInt(a,16) | 0x002000;
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			                
    			                    }
    			                    else { // -가 아닐때     
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+1;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			    		        }
    			            
    			                } // 즉시 주소지정방식 끝
    			        
    			                else if(Tokenizer.ADDR_MODE == 'N'){ // 간접주소지정방식이라면
    			        
    			                    pc += 3;
    			                    opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16);
    			            
    			                    if (opndAddr-pc > 4096 || opndAddr-pc < -4096){ // 베이스 상대 주소지정방식
    			                        opndAddr = (opndAddr-base) | 0x004000;
    			                    }
    			                    else{ // 프로그램 카운터 주소지정방식
    			             	        opndAddr = (opndAddr-pc) | 0x002000;
    			                    }
    			            
    			                    if (opndAddr < 0){ // - 부분 처리
    			                
    			                        String a = Integer.toHexString(opndAddr);
    			                        a = a.substring(a.length()-3);
    			                        opndAddr = Integer.parseInt(a,16) | 0x002000;
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			                    }
    			                    
    			                    else { // -가 아닐때 
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+2;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			    		        }
    			                }
    			        
    			                else if(Tokenizer.X_MODE == 'Y'){ // 인덱스 주소지정방식이라면
    			        
    			                    pc += 3;
    			                    opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16);
    			            
    			                    if (opndAddr-pc > 4096 || opndAddr-pc < -4096){ // 베이스 상대 주소지정방식
    			                        opndAddr = (opndAddr-base) | 0x00c000;
    			                    }
    			                    else{ // 프로그램 카운터 주소지정방식
    			             	        opndAddr = (opndAddr-pc) | 0x00a000;
    			                    }
    			            
    			                    if (opndAddr < 0){ // - 부분 처리
    			                
    			                        String a = Integer.toHexString(opndAddr);
    			                        a = a.substring(a.length()-3);
    			                        opndAddr = Integer.parseInt(a,16) | 0x002000;
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			                    }
    			                    
    			                    else { // -가 아닐때 
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			    	            }
    			                }
    			        
    			                else{ // 일반 3형식 이면
    			        	
    			        	        pc += 3;
    			        	        opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16);
    			            
    			                    if (opndAddr-pc > 4096 || opndAddr-pc < -4096){ // 베이스 상대 주소지정방식
    			                        opndAddr = (opndAddr-base) | 0x004000;
    			                    }
    			                    else{ // 프로그램 카운터 주소지정방식
    			                        opndAddr = (opndAddr-pc) | 0x002000;
    			                    }
    			            
    			                    if (opndAddr < 0){ // - 부분 처리
    			                
    			                        String a = Integer.toHexString(opndAddr);
    			                        a = a.substring(a.length()-3);
    			                        opndAddr = Integer.parseInt(a,16) | 0x002000;
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			                    }
    			                    
    			                    else { // -가 아닐때 
    			            	        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			    		        }
    			    	        }
    			            } // 3형식 끝
    			        }  // SIC/XE 모드 끝
    			        
    			        else if(Tokenizer.OPCODE.equals("RSUB")){ // "RSUB"일 경우(SIC)
    			    	
    			    	pc += 3;
    			    	opndAddr = 0;
    			    	codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16);
    			    	objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    	printListFile(interLine,objCode);
    			    	buffer += objCode;
    			    	objLength += 3;
    			        }
    			        
    			        else{ // SIC 모드이면
    			        
    			        	pc += 3;
    			        	codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16);
    			        	opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16);
    			        	objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr),4,0,0);
    			        	printListFile(interLine,objCode);
    			        	buffer += objCode;
    			        	objLength += 3;
    			        }
    			        
    			    } // OPCODE 이면 끝
    			    
    			    else if(Tokenizer.OPCODE.equals("RESW")){ // "RESW"일 경우  			    	
    			        
    			        pc += 3*(Integer.parseInt(Tokenizer.OPERAND));
    			        
    			        printListFile(interLine,""); // 리스트 파일 출력
    			            			        
    			        if(objLength != 0){
    			        	
    			        	// 이전 까지의 목적 코드 출력
    			        	outObj.println(("T"+Formatter(Integer.toHexString(objStartAddr),6,0,0)+
    			        	                   Formatter(Integer.toHexString(objLength),2,0,0)+buffer).toUpperCase());
    			        	Sicxe.atObj.append(("T"+Formatter(Integer.toHexString(objStartAddr),6,0,0)+
    			        	                   Formatter(Integer.toHexString(objLength),2,0,0)+buffer).toUpperCase());
    			        	Sicxe.atObj.append("\n");
    			        }
    			        
    			        objLength += 3*(Integer.parseInt(Tokenizer.OPERAND));;
    			        buffer = "";
    			        objStartAddr += objLength;
    			        objLength = 0;
    			    }
    			    
    			    else if(Tokenizer.OPCODE.equals("RESB")){ // "RESB"일 경우
    			    	
    			    	pc += (Integer.parseInt(Tokenizer.OPERAND));
    			    	
    			    	printListFile(interLine,""); // 리스트 파일 출력
    			    	    			    	
    			    	if(objLength != 0){
    			        	
    			        	// 이전 까지의 목적 코드 출력
    			        	outObj.println((("T"+Formatter(Integer.toHexString(objStartAddr),6,0,0)+
    			        	                   Formatter(Integer.toHexString(objLength),2,0,0)+buffer)).toUpperCase());
    			            Sicxe.atObj.append((("T"+Formatter(Integer.toHexString(objStartAddr),6,0,0)+
    			        	                   Formatter(Integer.toHexString(objLength),2,0,0)+buffer)).toUpperCase());
    			        	Sicxe.atObj.append("\n");
    			        }
    			        
    			        objLength += (Integer.parseInt(Tokenizer.OPERAND));
    			        buffer = "";
    			        objStartAddr += objLength;
    			        objLength = 0;
    			    }
    			    
    			    else if(Tokenizer.OPCODE.equals("WORD")){ // "WORD"일 경우
    			    	
    			    	pc += 3;
    			    	opndAddr = Integer.parseInt(Tokenizer.OPERAND);
    			    	objCode = Formatter(Integer.toHexString(opndAddr), 6, 0, 0);
    			    	
    			    	printListFile(interLine,objCode); // 리스트 파일 출력
    			    	
    			    	buffer += objCode;
    			    	objLength += 3;
    			    }
    			    
    			    else if(Tokenizer.OPCODE.equals("BYTE")){ // "BYTE"일 경우
    			    	
    			    	if(Tokenizer.BYTE_MODE == 'X'){ // 'X' 일 경우
    			    		
    			    		pc += 1;
    			    		objCode = Tokenizer.OPERAND;
    			    		printListFile(interLine,objCode);
    			    		buffer += objCode;
    			    		objLength += 1;
    			    	}
    			    	
    			    	else if(Tokenizer.BYTE_MODE == 'C'){ // 'C' 일 경우
    			    		
    			    		pc += Tokenizer.OPERAND.length();
    			    		objCode = "";
    			    		byte[] bval = null;
    			    		
    			    		try{
    			    			bval = Tokenizer.OPERAND.getBytes("ASCII");
    			    		}catch(UnsupportedEncodingException yee){}
    			    		
    			    		for (int i=0 ; i<Tokenizer.OPERAND.length() ; i++){
    			    			objCode += Integer.toHexString(bval[i]);
    			    		}
    			    		
    			    		printListFile(interLine,objCode);
    			    		buffer += objCode;
    			    		objLength += Tokenizer.OPERAND.length();
    			    	}
    			    	
    			    	else{ // 둘다 아닐 경우
    			    	
    			    		pc += 1;
    			    		opndAddr = Integer.parseInt(Tokenizer.OPERAND);
    			    		objCode = Formatter(Integer.toHexString(opndAddr), 2, 0, 0);
    			    		buffer += objCode;
    			    		objLength += 1;
    			        }
    			    }
    			    
    			    else if(Tokenizer.OPCODE.equals("BASE")){ // "BASE" 일 경우
    			    	printListFile(interLine,"");
    			    	base = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16);
    			    }
    			    
    			    if(objLength > 27){ // 목적코드 출력
    			    	
    			    	outObj.println(("T"+Formatter(Integer.toHexString(objStartAddr),6,0,0)+
    			    	                   Integer.toHexString(objLength)+buffer).toUpperCase());
    			    	Sicxe.atObj.append(("T"+Formatter(Integer.toHexString(objStartAddr),6,0,0)+
    			    	                   Integer.toHexString(objLength)+buffer).toUpperCase());
    			    	Sicxe.atObj.append("\n");
    			    	
    			    	buffer = "";
    			    	objStartAddr += objLength;
    			    	objLength = 0;
    			    }
    			    
       			} // 주석아니면 끝
       			
       			else { // 주석이면
    			   	printListFile(interLine,""); // 주석 출력
    			}
       			
       			// 다음 라인 읽고 분석
       			line = input.readLine();
       			interLine = interInput.readLine();
    			
    			Tokenizer.parse(line);
       			
    		} //'END'문 끝
    		
    		printListFile(interLine,""); // 마지막 리스트 파일 출력
    		
    		// 마지막 텍스트 레코드 출력
    		outObj.println(("T"+Formatter(Integer.toHexString(objStartAddr),6,0,0)+
    			    	       Formatter(Integer.toHexString(objLength),2,0,0)+buffer).toUpperCase());
    	    Sicxe.atObj.append(("T"+Formatter(Integer.toHexString(objStartAddr),6,0,0)+
    			    	       Formatter(Integer.toHexString(objLength),2,0,0)+buffer).toUpperCase());
            Sicxe.atObj.append("\n");
    	
    	    if(Tokenizer.XE_MODE == 'Y'){ // SIC/XE 일 경우 수정 레코드 출력
    	    	for(int i=0 ; i<count ; i++){
    	    		outObj.println(("M"+Formatter(Integer.toHexString(modifi[i]),6,0,0)+"05").toUpperCase()+"+"+mName);
    	    		Sicxe.atObj.append(("M"+Formatter(Integer.toHexString(modifi[i]),6,0,0)+"05").toUpperCase()+"+"+mName);
    	    		Sicxe.atObj.append("\n");
    	    	}
    	    }
    	    	
    	    // END 레코드 출력
    	    outObj.println("E"+Formatter(Integer.toHexString(startAddr),6,0,0));
    	    Sicxe.atObj.append("E"+Formatter(Integer.toHexString(startAddr),6,0,0));
    	       	                   
    		input.close();
    		outList.close();
    		outObj.close();
    		interInput.close();
          //---------------------------------------------------------------//
            // 초기화
            startAddr = progLength = objLength = opndAddr = codeAddr = count = 0;
          //---------------------------------------------------------------//
    	}catch(FileNotFoundException e){
    		System.err.println(e.toString());
    	}catch(IOException e){
    		System.err.println(e.toString());
    	}
    } 
    		
// --------------------------------------------------------------------------- //

    /* 레지스터가 몇번인지를 반환하는 메서드(패스 2용) */
    
    public String register(String register){
    	if(register.equals("A")){ return "0"; }
    	else if(register.equals("X")){ return "1"; }
    	else if(register.equals("L")){ return "2"; }
    	else if(register.equals("B")){ return "3"; }
    	else if(register.equals("S")){ return "4"; }
    	else if(register.equals("T")){ return "5"; }
    	else if(register.equals("F")){ return "6"; }
    	else if(register.equals("PC")){ return "8"; }
    	else if(register.equals("SW")){ return "9"; }
    	return "0";
    }
    	
    	
// --------------------------------------------------------------------------- //    		
    	
    /* OP테이블을 만드는 메서드 */
    
    public void opTable(){
    	
		OPTAB = new HashTable();
		
		try{
			// "opcode.txt" 파일을 가져와서 OP테이블을 만든다.
			BufferedReader in = new BufferedReader(new FileReader("opcode.txt"));
			String line = in.readLine();
						
			while(line != null){
				StringTokenizer t = new StringTokenizer(line, " ");
				
				String opcode = t.nextToken();
				String type = t.nextToken();
				String code = t.nextToken();
				
				OPTAB.put(opcode,type,code);
				line = in.readLine();
			}
			
			in.close();
			
        }catch(FileNotFoundException e){
			System.err.println(e.toString());
		}catch(IOException e){
			System.err.println(e.toString());
		}
	}
	
// --------------------------------------------------------------------------- //	
	
	/* 리스트 파일 출력 하는 메서드(참조) 
     * 중간파일 한라인, 목적코드,
     * 리스트 파일을 출력 할때 사용 한다.*/
     
    public void printListFile(String inter, String objcode){
    	
    	outList.println(inter+objcode.toUpperCase());
    	Sicxe.atList.append(inter+objcode.toUpperCase());
    	Sicxe.atList.append("\n");
    	
    }
    
// --------------------------------------------------------------------------- //    
    
    /* 임시 파일 출력하는 메서드 
     * 인자는 라인, 로케이션, 입력소스라인이다.*/
     
    public void printInterFile(int line, int lc, String codeLine){
    	
    	String strLine = Formatter(String.valueOf(line),4,1,0);
    	String strLC;
    	
    	if (lc != 0){
    		strLC = Formatter(Integer.toHexString(lc),6,1,0);
    	}
    	else{ // 로케이션 카운터를 출력 하지 않기 위한 부분
    		strLC = Formatter("",6,1,0);
    	}
    	
    	String strOut = Formatter(strLine+strLC.toUpperCase()+"    "+codeLine+"    ",40,1,1);
    	outInter.println(strOut);
    	Sicxe.atList.append(strOut);
    	Sicxe.atList.append("\n");
    }
    
// --------------------------------------------------------------------------- //    
    
    /* 지정된 문자열로 원하는 길이만큼 자리수를 채우는 함수
     * 인자로는 문자, 출력을원하는 문자길이, 
     * 플래그 : '0'이면 0으로 채우고 아니면 공백으로 채운다.
     * 정렬방식 : '0'이면 앞에 채우고 아니면 뒤에 채운다.
     * 의 4개의 인자가 있다. */
     
    public String Formatter(String str, int length, int opt, int align){
    	int fillLength = length - str.length();
    	String fillChar;
    	String retValue = "";
    	
    	if(opt == 0){ // '0'으로 채우기
    		fillChar = "0";
    	}
    	else{ // 공백으로 채우기
    		fillChar = " ";
    	}
    	
    	for(int i=0 ; i<fillLength ; i++){ // 빈문자열 만큼 채우기
    		retValue += fillChar;
    	}
    	
    	// 정렬방식의 결정
    	if(align == 0){ // 왼쪽에 채우기
    		return (retValue+str);
    	}
    	else{
    		return (str+retValue); // 오른쪽에 채우기
    	}
    }
}
    
        
