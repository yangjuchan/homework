/**
 * <p>Title: Sic_xe_assembler</p>
 * <p>Description: SIC_XE ������ �����</p>
 * <p>Copyright: Copyright (c) 2003 11. 7</p>
 * <p>Company: �����������б�</p>
 * @author ��ġȯ
 * @version 1.1(-_-v) (03.11.30)
 * 1.0������ ��ȭ : 1.�������̽� �߰�
 *                  2. sic/xe �� sic ���� ������ ����
 *                  3. resister()�޼��� register()�� �̸� ����
 * ���� ������ : 1. �ҽ� ���� ��ū�� 2���϶� ���� ��ó��
 */
 
import java.io.*;
import javax.swing.*;
import java.awt.*;
import javax.swing.border.*;
import java.awt.event.*;

public class Sicxe extends JFrame implements ActionListener{
	
	/* ������Ʈ ���� */
    
    JMenuBar jMenuBar1 = new JMenuBar();
    
    JMenu mFile = new JMenu();
    JMenuItem fOpen = new JMenuItem();
    
    JMenu mAssembler = new JMenu();
    JMenuItem aPass1 = new JMenuItem();
    JMenuItem aPass2 = new JMenuItem();
    JMenuItem aSymTable = new JMenuItem();
    JMenuItem aOpTable = new JMenuItem();
    
    JMenu mLoader = new JMenu();
    JMenuItem lPass1 = new JMenuItem();
    JMenuItem lPass2 = new JMenuItem();
    JMenuItem lEsTable = new JMenuItem();
    
    JMenu mHelp = new JMenu();
    JMenuItem hAbout = new JMenuItem();
    
    JTabbedPane jTabbedPane = new JTabbedPane();
    
    JPanel pSource = new JPanel();
    JTextArea tSource = new JTextArea();
    JPanel pAssembler = new JPanel();
    static JTextArea atList = new JTextArea();
    static JTextArea atObj = new JTextArea();
    JPanel pLoader = new JPanel();
    
    static JTable table;
        
    JScrollPane sp,sp2,sp3,sp4;
    
    TitledBorder titledBorder1;
    TitledBorder titledBorder2;
    
    GridLayout gridLayout1 = new GridLayout();
    
    private int FLAG=0;
    private String selectedFile;
    
    /* �����, �δ� Ŭ���� */
    
    private Sic_xe_assembler assm;
    private Loader loader;
    
// --------------------------------------------------------------------------- //

    /* ������ */    
    
    public Sicxe() {
        try {
            jbInit();
            start();
            setSize(800,600);
            Toolkit tk = Toolkit.getDefaultToolkit();
         	Dimension di = tk.getScreenSize();
		    Dimension di1 = this.getSize();
		    this.setLocation((int)(di.getWidth() / 2 - di1.getWidth() / 2), 
			                 (int)(di.getHeight() / 2 - di1.getHeight() / 2));

            setVisible(true);
            assm = new Sic_xe_assembler();
            loader = new Loader();
        }catch(Exception e) {e.printStackTrace();}
    }
    
// --------------------------------------------------------------------------- //

    /* �������̽� ���� �κ� */    
  
    private void jbInit() throws Exception {
        
        this.getContentPane().setBackground(SystemColor.text);
        this.setBackground(Color.lightGray);
        
        titledBorder1 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,Color.black),"����Ʈ ����");
        titledBorder2 = new TitledBorder(BorderFactory.createEtchedBorder(Color.white,Color.black),"������Ʈ ����");
        
        mFile.setText("File");
        fOpen.setText("Open");
        
        mAssembler.setText("Assembler");
        aPass1.setText("Pass1");
        aPass2.setText("Pass2");
        aSymTable.setText("Symbol Table");
        aOpTable.setText("OP Table");
        
        mLoader.setText("Loader");
        lPass1.setText("Pass1");
        lPass2.setText("Pass2");
        lEsTable.setText("ES Table");
        
        mHelp.setText("Help");
        hAbout.setText("About Sic/xe");
        
        pSource.setLayout(gridLayout1);
        
        tSource.setBackground(Color.lightGray);
        tSource.setBorder(BorderFactory.createEtchedBorder());
        tSource.setEditable(false);
        tSource.setText("");
              
        pAssembler.setLayout(gridLayout1);
        
        atList.setBackground(Color.lightGray);
        atList.setBorder(titledBorder1);
        atList.setEditable(false);
        atList.setText("");
                
        atObj.setBackground(Color.lightGray);
        atObj.setBorder(titledBorder2);
        atObj.setEditable(false);
        atObj.setText("");
        
        pLoader.setLayout(gridLayout1);
                       
        jMenuBar1.add(mFile);
        jMenuBar1.add(mAssembler);
        jMenuBar1.add(mLoader);
        jMenuBar1.add(mHelp);
        
        mFile.add(fOpen);
        
        mAssembler.add(aPass1);
        aPass1.setEnabled(false);
        mAssembler.add(aPass2);
        aPass2.setEnabled(false);
        mAssembler.addSeparator();
        mAssembler.add(aSymTable);
        aSymTable.setEnabled(false);
        mAssembler.add(aOpTable);
        aOpTable.setEnabled(false);
        
        mLoader.add(lPass1);
        lPass1.setEnabled(false);
        mLoader.add(lPass2);
        lPass2.setEnabled(false);
        mLoader.addSeparator();
        mLoader.add(lEsTable);
        lEsTable.setEnabled(false);
        
        mHelp.add(hAbout);
                
        jTabbedPane.addTab("Source File", pSource);
        sp = new JScrollPane(tSource);
        pSource.add(sp,null);
        
        jTabbedPane.addTab("Assembler", pAssembler);
        sp2 = new JScrollPane(atList);
        sp3 = new JScrollPane(atObj);
        pAssembler.add(sp2, null);
        pAssembler.add(sp3, null);
        
        jTabbedPane.addTab("Loader", pLoader);
        String title[] = {"�޸� �ּ�","","��","��",""};
        
        String data[][] = new String[300][5];
        int mem = 0x0FF0;
        for(int i=0 ; i<300 ; i++){
        	data[i][0] = Integer.toHexString(mem+=0x10).toUpperCase();
        }
        table = new JTable(data,title);
        sp4 = new JScrollPane(table);
        pLoader.add(sp4,null);
        pLoader.add(new JPanel(),null);
        
        this.getContentPane().add(jTabbedPane, BorderLayout.CENTER);
        this.getContentPane().add(jMenuBar1, BorderLayout.NORTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
    
// --------------------------------------------------------------------------- //

    /* �̺�Ʈ ���� */
    
    public void start(){
    	
    	fOpen.addActionListener(this);
    	aPass1.addActionListener(this);
    	aPass2.addActionListener(this);
    	aSymTable.addActionListener(this);
    	aOpTable.addActionListener(this);
    	lPass1.addActionListener(this);
    	lPass2.addActionListener(this);
    	lEsTable.addActionListener(this);
    	hAbout.addActionListener(this);
    }
    
// --------------------------------------------------------------------------- //

    /* �������̽� �̺�Ʈ ó�� */
    
    public void actionPerformed(ActionEvent e){
    	if(e.getSource() == fOpen){ // ���� ����
    		
    		// �ʱ�ȭ 
    		FileDialog fc = new FileDialog(this);
    		fc.setVisible(true);
    		tSource.setText("");
    		atList.setText("");
    		atObj.setText("");
    		aPass1.setEnabled(false);
    		aPass2.setEnabled(false);
    		aSymTable.setEnabled(false);
    		aOpTable.setEnabled(false);
    		lPass1.setEnabled(false);
    		lPass2.setEnabled(false);
    		lEsTable.setEnabled(false);
    		assm.SYMTAB.removeAll();
            loader.ESTAB.removeAll();
        	
    		for(int i=0 ; i<300 ; i++){
    			for(int j=1 ; j<5 ; j++){
    				table.setValueAt("",i,j);
    			}
    		}
    		try{
    			selectedFile = fc.getFile();
    			BufferedReader input = new BufferedReader(new FileReader(selectedFile));
    		    String imsi = input.readLine();
    		    while(imsi != null){
    			    tSource.append(imsi+"\n");
    			    imsi = input.readLine();
    			    FLAG=1;
    			    aPass1.setEnabled(true);
    			}
    		}catch(Exception eee){}
    	}
    	
    	else if(e.getSource() == aPass1){ // ����� �н�1
    		if(FLAG == 1){
    			assm.pass1(selectedFile);
    			FLAG=2;
    			aPass2.setEnabled(true);
    			aSymTable.setEnabled(true);
    			aOpTable.setEnabled(true);
    		}
    	}
    	
    	else if(e.getSource() == aPass2){ // ����� �н�2
    		if(FLAG == 2){
    			assm.pass2(selectedFile);
    			FLAG=3;
    			lPass1.setEnabled(true);
    		}
    	}
    	
    	else if(e.getSource() == aSymTable){ // �ɺ����̺���
    	    assm.SYMTAB.showSymTable();
    	}
    	
    	else if(e.getSource() == aOpTable){ // �������̺���
    	    assm.OPTAB.showOpTable();
    	}
    	
        else if(e.getSource() == lPass1){ // �δ� �н�1
        	if(FLAG == 3){
        		loader.pass1(selectedFile.substring(0,selectedFile.indexOf('.'))+".obj");
        		FLAG=4;
        		lPass2.setEnabled(true);
        		lEsTable.setEnabled(true);
        	}
        }
        
        else if(e.getSource() == lPass2){ // �δ� �н�2
        	if(FLAG == 4){
        		loader.pass2(selectedFile.substring(0,selectedFile.indexOf('.'))+".obj");
        		FLAG=0;
        	}
        }
        
        else if(e.getSource() == lEsTable){ // �ܺα�ȣ���̺� ����
            loader.ESTAB.showEsTable();
        }
        
        else if(e.getSource() == hAbout){ // ���� ����
            JOptionPane.showMessageDialog(this,
            "<html><pre>        Sic/xe ����� & �δ�</pre><pre>             by.Chyhwan(-_-v)</pre>"+
            "<pre>                      ver.1.1</pre></html>","About Sic/xe",
            JOptionPane.PLAIN_MESSAGE);
        }
    		
    }
    	

// --------------------------------------------------------------------------- //    
    public static void main(String[] args) {
        new Sicxe();
    }
}