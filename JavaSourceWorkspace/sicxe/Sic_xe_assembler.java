import java.io.*;
import java.util.*;

public class Sic_xe_assembler {

    /* �� ��� ���� ���� */
    
    private int startAddr = 0; // ���� �ּ� �ʱ�ȭ
    private int progLength = 0; // ���α׷� ����
    
    public HashTable SYMTAB,OPTAB; // �ɺ����̺�� OP���̺�
    
    private BufferedReader input = null; // �Է�(�ҽ�) ����
    private PrintWriter outInter = null; // �߰� ���� ���
    private PrintWriter outList = null; // LIST ���� ���
    private PrintWriter outObj = null; // OBJ ���� ���
               
// --------------------------------------------------------------------------- //    
    
    /* ����� ������!
     * ������ ���ڷ� �޴´�! */
     
    public Sic_xe_assembler(){
        
        opTable(); // op���̺� ���� 
        SYMTAB = new HashTable(); // �ɺ����̺� ����
    }

// --------------------------------------------------------------------------- //    
    
    /* �н� 1�� ����
     * ���ڷ� �ҽ� ������ �޴´�~ */
     
    public void pass1(String file){
    	
    	String line; // ���Ͽ��� ���δ����� �߶� �ޱ� ���� ����
    	int lc=0; // ��ġ �����̼� ī���� �ʱ�ȭ
    	int pc=0; // pcī���� �ʱ�ȭ
    	int count=1; // �߰����Ͽ� �� ���� ����
    	
    	// ù��° ������ �о� ��ū���� ����� �� �ʵ忡 ����
    	try{
    		input = new BufferedReader(new FileReader(file));
    		// '�̸�.int' ��� �߰� ���� ��� �κ�
    		outInter = new PrintWriter(new BufferedWriter(new FileWriter(file.substring(0,file.indexOf('.'))+".int")));
    		line = input.readLine();
    		
    		
    		// ������ �м�
    		Tokenizer.parse(line);
    		
    		// 'START' �����ڸ� ã��
    		if(Tokenizer.OPCODE.equals("START")){
    			// ���۷��忡�� �����ּҸ� �о� �����ϰ� lc �� ������
    			startAddr = Integer.parseInt(Tokenizer.OPERAND, 16);
    			lc = pc = startAddr;
    			
    			    			
    			printInterFile(count++, lc, line);
    			
    			// ���� ������ �а� �м�
    			line = input.readLine();
    			Tokenizer.parse(line);
    		}
    		
    		// 'END' �����ڸ� ���������� ����
    		while(!Tokenizer.OPCODE.equals("END")){
    			
    			if (!Tokenizer.LABEL.equals(".")){ // �ּ��� �ƴ϶�� ����
    				
    				// OPCODE�� üũ
    				// 4������ �ƴϸ� ������ 3�������� �ν�
    				if(Tokenizer.TYPE_MODE == 'F'){ // 4�����̸�
    					lc = pc += 4;
    					lc -= 4;
    				}
    				// @����� ������ ���� üũ(�������� EXCEPTION ������)@
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
    				 	// 'C', 'X'�� �����Ͽ� ����
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
    				
    				// 1,2,3 ���� ���� 
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
    				// OP���̺��� ���� ����� �����ڵ� �ƴϸ� ���� ǥ��
    				else {
    				 	System.out.println("���� �ڵ� ���� ������!");
    				 	return;
    				}
    				// �߰����� ���, ���̽��� ��� ���ϱ����� �ڵ�
    				if (!Tokenizer.OPCODE.equals("BASE")){
    					printInterFile(count++, lc, line);
    				}
    				else{
    					printInterFile(count++, 0, line);
    				}
    				
    				// LABEL�� üũ
    				if(Tokenizer.LABEL.length() > 0 ){ // ���̺��� ������
    				
    				    if(SYMTAB.search(Tokenizer.LABEL) == null){ // �ɺ����̺� ������
    				        SYMTAB.put(Tokenizer.LABEL,Integer.toHexString(lc));
    				    }
    				    else{ // �ɺ����̺� ������
    				    	System.out.println("���̺� �ߺ� ����!!!");
    				    	return;
    				    }
    				}
				}
    			else{ // �ּ��̶��
    				printInterFile(count++, 0, line);
    			}
    			
    			// ���� ���� �а� �м�
    			line = input.readLine();
    			Tokenizer.parse(line);
    		}
    		
    		printInterFile(count++,0 ,line); // END �� �߰����� ���
    		progLength = pc - startAddr; // ���α׷� ���̸� ����
    		input.close(); // �ҽ����� �ݱ�
    		outInter.close(); // �߰����� �ݱ�
          // --------------------------------------------------------- //
             // �ʱ�ȭ
            lc = pc = 0;
            count=1;
          // -------------------------------------------------------- //
    	}catch(FileNotFoundException e){ // ���� ���� üũ
    		System.out.println(e.toString());
    	}catch(IOException e){ // ����� ���� üũ
    		System.out.println(e.toString());
    	}
    }
    
// --------------------------------------------------------------------------- //

    /* �н� 2 ���� */
    
    public void pass2(String file){
    	
    	String buffer = ""; 
    	String objCode = "";
    	String line;
    	String interLine;
    	String mName=null;
    	BufferedReader interInput;
    	
    	int base, pc;
    	int objStartAddr = base = pc = startAddr; // ������Ʈ���� ���� �ּ�
    	int objLength = 0; // ������Ʈ ���� ����
    	int opndAddr = 0; // ���۷��� �ּ�
    	int codeAddr = 0; // �ڵ� �ּ�
    	int[] modifi = new int[10]; // ���� ���ڵ���� ���� ���� (10������ ����)
    	int count=0; // ���� ���ڵ�� ī����
    	
    	try{
    		
    		// ���� ����� ���ϰ�ü���� ����
    		input = new BufferedReader(new FileReader(file));
    		interInput = new BufferedReader(new FileReader(file.substring(0,file.indexOf('.'))+".int"));
    		outList = new PrintWriter(new BufferedWriter(new FileWriter(file.substring(0,file.indexOf('.'))+".lst")));
    		outObj = new PrintWriter(new BufferedWriter(new FileWriter(file.substring(0,file.indexOf('.'))+".obj")));
    		
    		line = input.readLine(); // �ҽ����� �о� ���� (�н� 2 ó����)
    		interLine = interInput.readLine(); // �߰������� �о� ����(����Ʈ ��¿�)
    		
    		Tokenizer.parse(line); // �ҽ����� �� �м�(�н� 2���� ����ϱ� ����)
    		
    		// 'START' ���þ ã��
    		if(Tokenizer.OPCODE.equals("START")){
    			
    			// �߰����� ���
    			printListFile(interLine,"");
    			
    			mName = Tokenizer.LABEL;
    			
    			// ������� ���
    			outObj.println(("H" + Formatter(Tokenizer.LABEL, 6, 1, 1) + 
    			                Formatter(Integer.toHexString(startAddr), 6, 0, 0) +
    			                Formatter(Integer.toHexString(progLength), 6, 0, 0)).toUpperCase());
    			Sicxe.atObj.append(("H" + Formatter(Tokenizer.LABEL, 6, 1, 1) + 
    			                Formatter(Integer.toHexString(startAddr), 6, 0, 0) +
    			                Formatter(Integer.toHexString(progLength), 6, 0, 0)).toUpperCase());
    			Sicxe.atObj.append("\n");
    			
    			// ���� ���� �а� �м�
    			line = input.readLine();
    			interLine = interInput.readLine();
    			
    			Tokenizer.parse(line);
    		}
    		
    		// 'END' �����ڸ� ã�������� ����
    		while(!Tokenizer.OPCODE.equals("END")){
    			
    			if(!Tokenizer.LABEL.equals(".")){ // �ּ��� �ƴϸ�
    			    
    			    if (OPTAB.getCode(Tokenizer.OPCODE) != null){ // OP���̺� ������
    			    	
    			    	if (Tokenizer.XE_MODE == 'Y'){ // SIC/XE ���
    			    		
    			    		if(Tokenizer.TYPE_MODE == 'F'){ // 4�����̸�
    			    		 	
    			    		 	if(Tokenizer.ADDR_MODE == 'I'){ // ����ּ���������̸�
    			    		
    			    		        if(SYMTAB.search(Tokenizer.OPERAND) != null){ // ���۷��尡 ���̺��̸�
    			    			        opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16) | 0x100000;
    			    			        modifi[count] = pc+1;
    			    			        count++;
    			    	            }
    			    		        else{ // ���۷��尡 ����̸�
    			    			        opndAddr = Integer.parseInt(Tokenizer.OPERAND) | 0x100000;
    			    		        }
    			    		
    			    		        pc += 4;
    			    		        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+1;
    			    		        objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 6, 0, 0);
    			    		        printListFile(interLine,objCode);
    			    		        buffer += objCode;
    			    		        objLength += 4;
    			    	        }
    			    	
    			    	        else if(Tokenizer.ADDR_MODE == 'N'){ // �����ּ���������̸�
    			    	
    			    		        pc += 4;
    			    		        opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16) | 0x100000;
    			    		        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+2;
    			    		        objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 6, 0, 0);
    			    		        printListFile(interLine,objCode);
    			    		        buffer += objCode;
    			    		        objLength += 4;
     			    	        }
    			    	
    			    	        else{ // �Ѵ� �ƴϸ�
    			    	
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
    			            } // 4�����̸� ��
    			            
    			            else if(OPTAB.getType(Tokenizer.OPCODE).equals("1")){ // 1�����̸�
    			    
    			    	        pc += 1;
    			    	        objCode = OPTAB.getCode(Tokenizer.OPCODE);
    			    	        printListFile(interLine,objCode);
    			    	        buffer += objCode;
    			    	        objLength += 1;
    			            }
    			    
    			            else if(OPTAB.getType(Tokenizer.OPCODE).equals("2")){ // 2�����̸�
    			        
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
    			    
    			            else if(Tokenizer.OPCODE.equals("RSUB")){ // "RSUB"�� ���
    			    	
    			    	        pc += 3;
    			    	        opndAddr = 0;
    			    	        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    	        objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    	        printListFile(interLine,objCode);
    			    	        buffer += objCode;
    			    	        objLength += 3;
    			            }
    			            
    			            else if(OPTAB.getType(Tokenizer.OPCODE).equals("3")){ // 3�����̸�
    			        
    			                if(Tokenizer.ADDR_MODE == 'I'){ // ����ּ���������̶��
    			        
    			                    pc += 3;
    			            
    			                    if(SYMTAB.search(Tokenizer.OPERAND) != null){ // ���۷��尡 ���̺��̸�
    			            
    			                        opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16);
    			                
    			                            if (opndAddr-pc > 4096 || opndAddr-pc < -4096){ // ���̽� ��� �ּ��������
    			                                opndAddr = (opndAddr-base) | 0x004000;
    			                            }
    			                            else{ // ���α׷� ī���� �ּ��������
    			                	            opndAddr = (opndAddr-pc) | 0x002000;
    			                            }
    			                    }
    			            
    			                    else{
    			            	        opndAddr = Integer.parseInt(Tokenizer.OPERAND,16);
    			                    }
    			                    
    			                    if (opndAddr < 0){ // - �κ� ó��
    			                
    			                        String a = Integer.toHexString(opndAddr);
    			                        a = a.substring(a.length()-3);
    			                        opndAddr = Integer.parseInt(a,16) | 0x002000;
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			                
    			                    }
    			                    else { // -�� �ƴҶ�     
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+1;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			    		        }
    			            
    			                } // ��� �ּ�������� ��
    			        
    			                else if(Tokenizer.ADDR_MODE == 'N'){ // �����ּ���������̶��
    			        
    			                    pc += 3;
    			                    opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16);
    			            
    			                    if (opndAddr-pc > 4096 || opndAddr-pc < -4096){ // ���̽� ��� �ּ��������
    			                        opndAddr = (opndAddr-base) | 0x004000;
    			                    }
    			                    else{ // ���α׷� ī���� �ּ��������
    			             	        opndAddr = (opndAddr-pc) | 0x002000;
    			                    }
    			            
    			                    if (opndAddr < 0){ // - �κ� ó��
    			                
    			                        String a = Integer.toHexString(opndAddr);
    			                        a = a.substring(a.length()-3);
    			                        opndAddr = Integer.parseInt(a,16) | 0x002000;
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			                    }
    			                    
    			                    else { // -�� �ƴҶ� 
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+2;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			    		        }
    			                }
    			        
    			                else if(Tokenizer.X_MODE == 'Y'){ // �ε��� �ּ���������̶��
    			        
    			                    pc += 3;
    			                    opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16);
    			            
    			                    if (opndAddr-pc > 4096 || opndAddr-pc < -4096){ // ���̽� ��� �ּ��������
    			                        opndAddr = (opndAddr-base) | 0x00c000;
    			                    }
    			                    else{ // ���α׷� ī���� �ּ��������
    			             	        opndAddr = (opndAddr-pc) | 0x00a000;
    			                    }
    			            
    			                    if (opndAddr < 0){ // - �κ� ó��
    			                
    			                        String a = Integer.toHexString(opndAddr);
    			                        a = a.substring(a.length()-3);
    			                        opndAddr = Integer.parseInt(a,16) | 0x002000;
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			                    }
    			                    
    			                    else { // -�� �ƴҶ� 
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			    	            }
    			                }
    			        
    			                else{ // �Ϲ� 3���� �̸�
    			        	
    			        	        pc += 3;
    			        	        opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16);
    			            
    			                    if (opndAddr-pc > 4096 || opndAddr-pc < -4096){ // ���̽� ��� �ּ��������
    			                        opndAddr = (opndAddr-base) | 0x004000;
    			                    }
    			                    else{ // ���α׷� ī���� �ּ��������
    			                        opndAddr = (opndAddr-pc) | 0x002000;
    			                    }
    			            
    			                    if (opndAddr < 0){ // - �κ� ó��
    			                
    			                        String a = Integer.toHexString(opndAddr);
    			                        a = a.substring(a.length()-3);
    			                        opndAddr = Integer.parseInt(a,16) | 0x002000;
    			                        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			                    }
    			                    
    			                    else { // -�� �ƴҶ� 
    			            	        codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16)+3;
    			    		            objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    		            printListFile(interLine,objCode);
    			    		            buffer += objCode;
    			    		            objLength += 3;
    			    		        }
    			    	        }
    			            } // 3���� ��
    			        }  // SIC/XE ��� ��
    			        
    			        else if(Tokenizer.OPCODE.equals("RSUB")){ // "RSUB"�� ���(SIC)
    			    	
    			    	pc += 3;
    			    	opndAddr = 0;
    			    	codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16);
    			    	objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr), 4, 0, 0);
    			    	printListFile(interLine,objCode);
    			    	buffer += objCode;
    			    	objLength += 3;
    			        }
    			        
    			        else{ // SIC ����̸�
    			        
    			        	pc += 3;
    			        	codeAddr = Integer.parseInt(OPTAB.getCode(Tokenizer.OPCODE),16);
    			        	opndAddr = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16);
    			        	objCode = Formatter(Integer.toHexString(codeAddr),2,0,0)+Formatter(Integer.toHexString(opndAddr),4,0,0);
    			        	printListFile(interLine,objCode);
    			        	buffer += objCode;
    			        	objLength += 3;
    			        }
    			        
    			    } // OPCODE �̸� ��
    			    
    			    else if(Tokenizer.OPCODE.equals("RESW")){ // "RESW"�� ���  			    	
    			        
    			        pc += 3*(Integer.parseInt(Tokenizer.OPERAND));
    			        
    			        printListFile(interLine,""); // ����Ʈ ���� ���
    			            			        
    			        if(objLength != 0){
    			        	
    			        	// ���� ������ ���� �ڵ� ���
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
    			    
    			    else if(Tokenizer.OPCODE.equals("RESB")){ // "RESB"�� ���
    			    	
    			    	pc += (Integer.parseInt(Tokenizer.OPERAND));
    			    	
    			    	printListFile(interLine,""); // ����Ʈ ���� ���
    			    	    			    	
    			    	if(objLength != 0){
    			        	
    			        	// ���� ������ ���� �ڵ� ���
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
    			    
    			    else if(Tokenizer.OPCODE.equals("WORD")){ // "WORD"�� ���
    			    	
    			    	pc += 3;
    			    	opndAddr = Integer.parseInt(Tokenizer.OPERAND);
    			    	objCode = Formatter(Integer.toHexString(opndAddr), 6, 0, 0);
    			    	
    			    	printListFile(interLine,objCode); // ����Ʈ ���� ���
    			    	
    			    	buffer += objCode;
    			    	objLength += 3;
    			    }
    			    
    			    else if(Tokenizer.OPCODE.equals("BYTE")){ // "BYTE"�� ���
    			    	
    			    	if(Tokenizer.BYTE_MODE == 'X'){ // 'X' �� ���
    			    		
    			    		pc += 1;
    			    		objCode = Tokenizer.OPERAND;
    			    		printListFile(interLine,objCode);
    			    		buffer += objCode;
    			    		objLength += 1;
    			    	}
    			    	
    			    	else if(Tokenizer.BYTE_MODE == 'C'){ // 'C' �� ���
    			    		
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
    			    	
    			    	else{ // �Ѵ� �ƴ� ���
    			    	
    			    		pc += 1;
    			    		opndAddr = Integer.parseInt(Tokenizer.OPERAND);
    			    		objCode = Formatter(Integer.toHexString(opndAddr), 2, 0, 0);
    			    		buffer += objCode;
    			    		objLength += 1;
    			        }
    			    }
    			    
    			    else if(Tokenizer.OPCODE.equals("BASE")){ // "BASE" �� ���
    			    	printListFile(interLine,"");
    			    	base = Integer.parseInt(SYMTAB.search(Tokenizer.OPERAND),16);
    			    }
    			    
    			    if(objLength > 27){ // �����ڵ� ���
    			    	
    			    	outObj.println(("T"+Formatter(Integer.toHexString(objStartAddr),6,0,0)+
    			    	                   Integer.toHexString(objLength)+buffer).toUpperCase());
    			    	Sicxe.atObj.append(("T"+Formatter(Integer.toHexString(objStartAddr),6,0,0)+
    			    	                   Integer.toHexString(objLength)+buffer).toUpperCase());
    			    	Sicxe.atObj.append("\n");
    			    	
    			    	buffer = "";
    			    	objStartAddr += objLength;
    			    	objLength = 0;
    			    }
    			    
       			} // �ּ��ƴϸ� ��
       			
       			else { // �ּ��̸�
    			   	printListFile(interLine,""); // �ּ� ���
    			}
       			
       			// ���� ���� �а� �м�
       			line = input.readLine();
       			interLine = interInput.readLine();
    			
    			Tokenizer.parse(line);
       			
    		} //'END'�� ��
    		
    		printListFile(interLine,""); // ������ ����Ʈ ���� ���
    		
    		// ������ �ؽ�Ʈ ���ڵ� ���
    		outObj.println(("T"+Formatter(Integer.toHexString(objStartAddr),6,0,0)+
    			    	       Formatter(Integer.toHexString(objLength),2,0,0)+buffer).toUpperCase());
    	    Sicxe.atObj.append(("T"+Formatter(Integer.toHexString(objStartAddr),6,0,0)+
    			    	       Formatter(Integer.toHexString(objLength),2,0,0)+buffer).toUpperCase());
            Sicxe.atObj.append("\n");
    	
    	    if(Tokenizer.XE_MODE == 'Y'){ // SIC/XE �� ��� ���� ���ڵ� ���
    	    	for(int i=0 ; i<count ; i++){
    	    		outObj.println(("M"+Formatter(Integer.toHexString(modifi[i]),6,0,0)+"05").toUpperCase()+"+"+mName);
    	    		Sicxe.atObj.append(("M"+Formatter(Integer.toHexString(modifi[i]),6,0,0)+"05").toUpperCase()+"+"+mName);
    	    		Sicxe.atObj.append("\n");
    	    	}
    	    }
    	    	
    	    // END ���ڵ� ���
    	    outObj.println("E"+Formatter(Integer.toHexString(startAddr),6,0,0));
    	    Sicxe.atObj.append("E"+Formatter(Integer.toHexString(startAddr),6,0,0));
    	       	                   
    		input.close();
    		outList.close();
    		outObj.close();
    		interInput.close();
          //---------------------------------------------------------------//
            // �ʱ�ȭ
            startAddr = progLength = objLength = opndAddr = codeAddr = count = 0;
          //---------------------------------------------------------------//
    	}catch(FileNotFoundException e){
    		System.err.println(e.toString());
    	}catch(IOException e){
    		System.err.println(e.toString());
    	}
    } 
    		
// --------------------------------------------------------------------------- //

    /* �������Ͱ� ��������� ��ȯ�ϴ� �޼���(�н� 2��) */
    
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
    	
    /* OP���̺��� ����� �޼��� */
    
    public void opTable(){
    	
		OPTAB = new HashTable();
		
		try{
			// "opcode.txt" ������ �����ͼ� OP���̺��� �����.
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
	
	/* ����Ʈ ���� ��� �ϴ� �޼���(����) 
     * �߰����� �Ѷ���, �����ڵ�,
     * ����Ʈ ������ ��� �Ҷ� ��� �Ѵ�.*/
     
    public void printListFile(String inter, String objcode){
    	
    	outList.println(inter+objcode.toUpperCase());
    	Sicxe.atList.append(inter+objcode.toUpperCase());
    	Sicxe.atList.append("\n");
    	
    }
    
// --------------------------------------------------------------------------- //    
    
    /* �ӽ� ���� ����ϴ� �޼��� 
     * ���ڴ� ����, �����̼�, �Է¼ҽ������̴�.*/
     
    public void printInterFile(int line, int lc, String codeLine){
    	
    	String strLine = Formatter(String.valueOf(line),4,1,0);
    	String strLC;
    	
    	if (lc != 0){
    		strLC = Formatter(Integer.toHexString(lc),6,1,0);
    	}
    	else{ // �����̼� ī���͸� ��� ���� �ʱ� ���� �κ�
    		strLC = Formatter("",6,1,0);
    	}
    	
    	String strOut = Formatter(strLine+strLC.toUpperCase()+"    "+codeLine+"    ",40,1,1);
    	outInter.println(strOut);
    	Sicxe.atList.append(strOut);
    	Sicxe.atList.append("\n");
    }
    
// --------------------------------------------------------------------------- //    
    
    /* ������ ���ڿ��� ���ϴ� ���̸�ŭ �ڸ����� ä��� �Լ�
     * ���ڷδ� ����, ��������ϴ� ���ڱ���, 
     * �÷��� : '0'�̸� 0���� ä��� �ƴϸ� �������� ä���.
     * ���Ĺ�� : '0'�̸� �տ� ä��� �ƴϸ� �ڿ� ä���.
     * �� 4���� ���ڰ� �ִ�. */
     
    public String Formatter(String str, int length, int opt, int align){
    	int fillLength = length - str.length();
    	String fillChar;
    	String retValue = "";
    	
    	if(opt == 0){ // '0'���� ä���
    		fillChar = "0";
    	}
    	else{ // �������� ä���
    		fillChar = " ";
    	}
    	
    	for(int i=0 ; i<fillLength ; i++){ // ���ڿ� ��ŭ ä���
    		retValue += fillChar;
    	}
    	
    	// ���Ĺ���� ����
    	if(align == 0){ // ���ʿ� ä���
    		return (retValue+str);
    	}
    	else{
    		return (str+retValue); // �����ʿ� ä���
    	}
    }
}
    
        
