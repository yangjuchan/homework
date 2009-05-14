import java.io.*;
import java.util.*;

/* ���� ���α׷��� ������ �޾Ƽ�
 * ������ LABEL, OPCODE, OPERAND�� �����ϰ�
 * BYTE �����ڸ� ���� X ���� C ������ ���� �ϸ�
 * 4�������� �ƴ����� ���� �ϸ�
 * �����ּ� ���� ������� ���� �ּ����� ��������� ���� �ϸ�
 * �ε��� �������͸� ����ϴ����� ���� �ϴ� Ŭ���� */
 
class Tokenizer{
        
        /* ������ ��� ǥ�� */
        
        // OPCODE �� BYTE �϶� OPERAND�� 'X' ���� 'C' ���� ���� 
        static char BYTE_MODE = 'N';
        // OPCODE �� 4�������� : 'F' �ƴ��� : 'T' ���� 
        static char TYPE_MODE = 'T';
        // �ּ� ���� ����� ���� ���� : 'I' �������� : 'N' �Ϲ������� : 'D' ����
        static char ADDR_MODE = 'D';
        // �ε��� �������͸� ��� �ϴ��� : 'Y' �Ȼ���ϴ��� 'N' ����
        static char X_MODE = 'N';
        
        static char XE_MODE = 'N'; // SIC�� SIC/XE ���� ����
        
// --------------------------------------------------------------------------- //        
        
        /* �� ��� ������ ����ƽ���� �����Ѵ� */
        static String LABEL = "";
        static String OPCODE = "";
        static String OPERAND = "";
        static int tokenNum = 0;
        
// --------------------------------------------------------------------------- //        
        
        /* ������ ������ �Ľ� �ϴ� �޼���μ� ����ƽ�� �����Ѵ�. */
        
        static void parse(String str){
        	
        	StringTokenizer t = new StringTokenizer(str, " ");
        	tokenNum = t.countTokens();
        	String firstElement = t.nextToken();
        	
        	// �ּ��κп� ���� ó��
        	if (firstElement.equals(".")){
        		LABEL = ".";
        		return;
        	}
        	
        	// ���������� LABEL, OPCODE, OPERAND �� �� �޾����� ó���κ�
        	if(tokenNum ==3){
        		LABEL = firstElement;
        		OPCODE = t.nextToken();
        		OPERAND = t.nextToken();
        	}
        	
        	// 2�� �� �޾������ ó��
        	else if(tokenNum == 2){
        		//if(OPTAB.getCode(firstElement) != null){ // ���̺��� �������
        			LABEL = "";
        			OPCODE = firstElement;
        			OPERAND = t.nextToken();
        		//}
        		/*else{ // ���۷��尡 �������
        			LABEL = firstElement;
        			OPCODE = t.nextToken();
        			OPERAND = "";
        		}*/
        	}
        	
        	// 1���� ��ɰ� ���� OPCODE �� ���� �ϴ� �κ��� ó��
        	else if(tokenNum == 1){
        		LABEL = "";
        		OPCODE = firstElement;
        		OPERAND = "";
        	}
        	
            // --------------------------------------------------------------- //
                    	
         	if(tokenNum >= 2){ // @��ū 2�� �̻��̶�� ó�� ���ϸ� �����߻�@
        	    
        	    /* OPERAND ���� ���� ����ǥ�� �߰� �Ǹ�
        	     * OPCODE �κп� BYTE�� ��� ���� �˾� ä��
        	     * �� �κ��� ó�� */
        	
           	    // ���� ����ǥ�� ��ġ�� ���� �ϴ� ����
           	    int byteModeCheck = OPERAND.indexOf("'");
        	
        	    // ���� ����ǥ�� �߰������� ó�� ��, BYTE ������ �϶� ó��
        	    if (byteModeCheck >= 0){
        		    // 'X' �� 'C' �� ���� �Ǵ� ����
        		    String byteMode = OPERAND.substring(0, byteModeCheck);
        		    // ���� OPERAND �� ���� �Ǵ� ����
        		    String byteOperand = OPERAND.substring(byteModeCheck+1,OPERAND.lastIndexOf("'"));
        		
        		    // 'X' �϶��� ó��
        		    if(byteMode.equals("X")){
        			    BYTE_MODE = 'X';
        			    OPERAND = byteOperand;
        		    }
        		    // 'C' �϶��� ó��
        		    else if(byteMode.equals("C")){
        			    BYTE_MODE = 'C';
        			    OPERAND = byteOperand;
           		    }
        	    }
        	    // BYTE �� �̴Ҷ� ó��
        	    else{
        		  	BYTE_MODE = 'N';
        		}
        		
        	// --------------------------------------------------------------- //	
        	
        	    /* 4�����϶� ó�� �κ� '+' �� ���� �Ͽ� üũ �Ѵ�. */
        	
        	    // OPCODE ù �ܾ ������ ����
        	    String typeMode = OPCODE.substring(0,1);
        	    // OPCODE ù �ܾ + �̸� 4���� ��� ���� 
        	    if (typeMode.equals("+")){
          		    TYPE_MODE = 'F';
          		    XE_MODE = 'Y';
        		    OPCODE = OPCODE.substring(1);
        	    }
        	    // 4������ �ƴҶ� ó��
        	    else{
        		    TYPE_MODE = 'T';
        	    }
        	    
        	// --------------------------------------------------------------- //    
        	
        	    /* �ּ� ���� ����� �����Ͽ� ���� ���ش�(���,����,����Ʈ). */
        	
        	    // OPERAND ù���� ���ڸ� ����
        	    String addrMode = OPERAND.substring(0,1);
        	    // ��� �ּ� ���� ��� ó��
        	    if (addrMode.equals("#")){
        		    ADDR_MODE = 'I';
        		    XE_MODE = 'Y';
        		    OPERAND = OPERAND.substring(1);
        	    }
        	    // ���� �ּ� ���� ��� ó��
        	    else if(addrMode.equals("@")){
        		    ADDR_MODE = 'N';
        		    XE_MODE = 'Y';
        		    OPERAND = OPERAND.substring(1);
        	    }
        	    // �Ѵ� �ƴϸ� 
        	    else{
        		    ADDR_MODE = 'D';
        	    }
        	    
        	// --------------------------------------------------------------- //    
        	
        	    /* �ε��� ���������� ��� ���� �κ� ó��
        	     * �����ڵ忡�� ',X' �κ��� �� �ٿ� ��߸� ó�� ���� */
        	
          	    // ',X' �κ��� �ε��� ���� ���� �ϴ� ����
        	    int xModeCheck = OPERAND.indexOf(",X");
         	
        	    // ',X' �κ��� ������, �� �ε��� �������͸� ��� �ϸ�
        	    if (xModeCheck >= 0){
         		    X_MODE = 'Y';
        		    OPERAND = OPERAND.substring(0,xModeCheck);
        	    }
        	    // �ε��� �������͸� ��� ���ϸ�
        	    else{
          		    X_MODE = 'N';
        	    }
            }
        }
 }