package de.mud.jta.plugin;

import de.mud.jta.Plugin;
import de.mud.jta.PluginBus;
import de.mud.jta.FilterPlugin;
import de.mud.jta.VisualPlugin;

import de.mud.bsx.BSXDisplay;

import java.io.IOException;

import java.awt.Component;
import java.awt.Menu;
import java.awt.Panel;
import java.awt.FlowLayout;

/**
 * ultrahighspeed-BSX-command-parser as Plugin for JTA 2.0 (RC1)
 @version Java 1.0
 @author  Thomas Kriegelstein (tk4@rb.mud.de)
 */
public class BSX extends Plugin 
    implements FilterPlugin, VisualPlugin {
    
    /** the canvas that contains the Gfx */
    protected BSXDisplay visual = new BSXDisplay();
    /** the container for this plugin */
    protected Panel panel = new Panel(new FlowLayout(FlowLayout.CENTER));
    /* the BSX Commands to be understood */
    private static final byte[] RFS = "@RFS".getBytes();
    private static final byte[] SCE = "@SCE".getBytes();
    private static final byte[] VIO = "@VIO".getBytes();
    private static final byte[] DFS = "@DFS".getBytes();
    private static final byte[] DFO = "@DFO".getBytes();
    private static final byte[] RMO = "@RMO".getBytes();
    private static final byte[] TMS = "@TMS".getBytes();
    private static final byte[] RQV = "@RQV".getBytes();

    /**
     * initialize the parser
     */
    public BSX(PluginBus bus, final String id) {
	super(bus, id);
	panel.add(visual);
	reset();
    }
    
    public Component getPluginVisual() {
	return panel;
    }
    
    public Menu getPluginMenu() {
	return null;
    }
    
    FilterPlugin source;
    
    public void setFilterSource(FilterPlugin source) {
	this.source = source;
    }
    
    public int read(byte[] b) throws IOException {
	int len;
	len=source.read(b);
	len=parse(b,len);
	return len;
    }

    public void write(byte[] b) throws IOException {
	source.write(b);
    }

    public void write(String s) throws IOException {
	write(s.getBytes());
    }
    /* ********************************************************************* */
    /* "Ultrahighspeed" Statemachine for BSX Sequences                       */
    /* ********************************************************************* */
    /*                         Buffers & States                              */
    /* ********************************************************************* */
    private byte[]   cmd = new byte[4];      // command                      */
    private int   cmdlen = 0;                // length of command            */
    private byte[]    id = new byte[64];     // identifier                   */
    private int    idlen = 0;                // length of identifier         */
    private String   obj = null;             // string representation of id  */
    private int[][] data = null;             // data                         */
    private byte[]   hex = new byte[2];      // 00-FF integer                */
    private int   hexlen = 0;                // length of integer            */
    private byte[]   res = new byte[4096];   // storage for parse-result     */
    /* ********************************************************************* */
    private int    polys = 0; // 0..31        number of polygons in data     */
    private int    edges = 0; // 0..31        number of edges in data        */
    private int     poly = 0; // 0..31        current polygon in data        */
    private int      pos = 0; // 0..edges*2+1 current position in polygon    */
    private int     xpos = 0; // 0..15        xpos of object                 */
    private int     ypos = 0; // 0..7         ypos of object                 */
    private int    state = 0; // 0..7         what to do next                */
    /* ********************************************************************* */
    /* 0 read until next '@'                                                 */
    /* 1 read command                                                        */
    /* 2 read identifier                                                     */
    /* 3 read polygoncount                                                   */
    /* 4 read edgecount                                                      */
    /* 5 read polygondata                                                    */
    /* 6 read xpos                                                           */
    /* 7 read ypos                                                           */
    /* ********************************************************************* */

    /**
     * reset the parser
     */
    public void reset()
    {
	cmdlen=idlen=hexlen=0;
	data=null;
	obj=null;
	polys=edges=poly=pos=xpos=ypos=state=0;
    }
    /**
     * parse the input buffer
     @param b      input buffer byte array
     @param length count of valid bytes in buffer
     @return       new length of valid bytes in buffer
    */
    private int parse(byte[] b, int length) throws IOException
    {
	int index,resindex;

	for (index=resindex=0;index<length;index++)
	{
	    switch(state)
		{
		case 0: // read until next @
		    if ((char)b[index]=='@')
			{
			    cmd[cmdlen++]=b[index];
			    state=1;
			}
		    else
			{
			    res[resindex++]=b[index];
			}
		    break;
		case 1: // read command
		    if ((char)b[index]=='@')
			{
			    for(int i=0;i<cmdlen;i++)
				res[resindex++]=cmd[i];
			    cmdlen=0;
			    cmd[cmdlen++]=b[index];
			}
		    else
			{
			    cmd[cmdlen++]=b[index];
			    if (cmdlen==4)
				{
				    if (equals(cmd,RFS))
					{
					    visual.refreshScene();
					    reset();
					}
				    else if (equals(cmd,RQV))
					{
					    write("#VER Java 1.0\n");
					    reset();
					}
				    else if (equals(cmd,SCE))
					{
					    state=2;
					}
				    else if (equals(cmd,VIO))
					{
					    state=2;
					}
				    else if (equals(cmd,DFO))
					{
					    state=2;
					}
				    else if (equals(cmd,RMO))
					{
					    state=2;
					}
				    else if (equals(cmd,DFS))
					{
					    state=2;
					}
				    else if (equals(cmd,TMS))
					{
					
					    byte[] temp="\n\n\tTerminate Session!\n\n".getBytes();
					    for (int i=0;i<temp.length;i++)
						res[resindex++]=temp[i];
					    reset();
					}
				    else
					{
					    for (int i=0;i<cmdlen;i++)
						res[resindex++]=cmd[i];
					    reset();
					}
				}
			}
		    break;
		case 2: // read identifier
		    if ((char)b[index]=='@')
			{
			    for(int i=0;i<cmdlen;i++)
				res[resindex++]=cmd[i];
			    for(int i=0;i<idlen;i++)
				res[resindex++]=id[i];
			    cmdlen=0;
			    cmd[cmdlen++]=b[index];
			    idlen=0;
			    state=1;
			}
		    else if ((char)b[index]!='.')
			{
			    id[idlen++]=b[index];
			}
		    else
			{
			    obj=new String(id,0,idlen);
			    if (equals(cmd,SCE))
				{
				    String query=visual.showScene(obj);
				    if (query!=null)
					write(query);
				    reset();
				}
			    else if (equals(cmd,VIO))
				{
				    state=6;
				}
			    else if (equals(cmd,RMO))
				{
				    visual.removeObject(obj);
				    reset();
				}
			    else if (equals(cmd,DFS))
				{
				    state=3;
				}
			    else if (equals(cmd,DFO))
				{
				    state=3;
				}
			}
		    break;
		case 3: // read polygoncount
		    hex[hexlen++]=b[index];
		    if (hexlen==2)
			{
			    polys=hexToInt(hex);
			    data=new int[polys][];
			    state=4;
			    hexlen=0;
			}
		    break;
		case 4: // read edgecount
		    hex[hexlen++]=b[index];
		    if (hexlen==2)
			{
			    edges=hexToInt(hex);
			    data[poly]=new int[1+edges*2];
			    state=5;
			    hexlen=0;
			}
		    break;
		case 5: // read polygondata
		    hex[hexlen++]=b[index];
		    if (hexlen==2)
			{
			    int c=hexToInt(hex);
			    data[poly][pos]=c;
			    hexlen=0;
			    pos++;
			    if (pos==edges*2+1)
				{
				    poly++;
				    state=4;
				    pos=0;
				    if (poly==polys)
					{
					    if (equals(cmd,DFS))
						{
						    visual.defineScene(obj,
								       data);
						}
					    else if (equals(cmd,DFO))
						{
						    visual.defineObject(obj,
									data);
						}
					    reset();
					}
				}
			}
		    break;
		case 6: // read xpos
		    hex[hexlen++]=b[index];
		    if (hexlen==2)
			{
			    xpos=hexToInt(hex);
			    state=7;
			    hexlen=0;
			}
		    break;
		case 7: // read ypos
		    hex[hexlen++]=b[index];
		    if (hexlen==2)
			{
			    ypos=hexToInt(hex);
			    String query=visual.showObject(obj,xpos,ypos);
			    if (query!=null)
				write(query);
			    reset();
			}
		    break;
		}
	}
	System.arraycopy(res,0,b,0,resindex);
	return resindex;
    }
    /**
     * compares two byte[]
     @return true if they contain the same values
     */
    private boolean equals(byte[] a, byte[] b)
    {
	for (int i=0;i<a.length&&i<b.length;i++)
	    if (a[i]!=b[i]) return false;
	return a.length==b.length;
    }
    /**
     * computes an integer from an byte[2] containing a 
     * hexadecimal representation in capitol letters (0-9,A-F)
     */
    private int hexToInt(byte[] b)
    {
	int  f=0,g=0;
	char h=0,i=0;

	h=(char)b[0];
	i=(char)b[1];
	if (h>='A'&&h<='F')
	    f=h-'A'+10;
	else if (h>='0'&&h<='9')
	    f=h-'0';
	if (i>='A'&&i<='F')
	    g=i-'A'+10;
	else if (i>='0'&&i<='9')
	    g=i-'0';
	return (int)(f*16+g);
    }
}
