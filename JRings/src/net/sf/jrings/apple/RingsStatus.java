package net.sf.jrings.apple;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.sf.j3d.network.NetworkClient;
import net.sf.j3d.network.Server;
import net.sf.j3d.network.db.Client;

import com.apple.cocoa.application.NSApplication;
import com.apple.cocoa.application.NSMenuItem;
import com.apple.cocoa.application.NSTextField;
import com.apple.cocoa.foundation.NSSelector;
import com.apple.mrj.MRJApplicationUtils;
import com.apple.mrj.MRJFileUtils;

public class RingsStatus extends PrintStream {
	private NSMenuItem statusItem, terminalItem, behaviorItem, logItem;
	private JFrame logWindow;
//	private JTextArea log;
	private NSTextField log;
	private byte b;
	
	public RingsStatus() throws FileNotFoundException {
		super("rings.log");
		
		if (AppleSettings.application == null)
			AppleSettings.application = new NSApplication();
		
		this.log = new NSTextField();
//		this.log = new JTextArea(25, 40);
		
//		this.logWindow = new JFrame("Rings Log");
//		this.logWindow.setSize(200, 120);
//		this.logWindow.getContentPane().add(new JScrollPane(this.log));
		
//		System.setOut(this);
//		this.logWindow.setVisible(true);
		
		if (Client.getCurrentClient() == null)
			NetworkClient.main(new String[0]);
		
		this.statusItem = new NSMenuItem();
		this.statusItem.setTitle("Status");
		this.statusItem.setAction(new NSSelector("handleAction", new Class[] {NSMenuItem.class}));
		
		this.terminalItem = new NSMenuItem();
		this.terminalItem.setTitle("Terminal");
		this.terminalItem.setAction(new NSSelector("handleAction", new Class[] {NSMenuItem.class}));
		
		this.logItem = new NSMenuItem();
		this.logItem.setTitle("Log");
		this.logItem.setAction(new NSSelector("handleAction", new Class[] {NSMenuItem.class}));
		
		this.behaviorItem = new NSMenuItem();
		this.behaviorItem.setTitle("Behavior");
		this.behaviorItem.setAction(new NSSelector("handleAction", new Class[] {NSMenuItem.class}));
		this.addBehaviors();
	}
	
	public NSMenuItem[] getMenuItems() {
		return new NSMenuItem[] {this.logItem, this.statusItem, this.terminalItem, this.behaviorItem};
	}
	
	protected void addBehaviors() {
		NSMenuItem bonjour = new NSMenuItem();
		bonjour.setTitle("Bonjour");
		bonjour.setAction(new NSSelector("runBonjour", new Class[] {}));
	}
	
	public void runBonjour() {
		boolean v = this.logWindow.isVisible();
		this.logWindow.setVisible(true);
		BonjourBehavior b = new BonjourBehavior();
		b.behave(Client.getCurrentClient().getServer(), System.out);
		this.logWindow.setVisible(v);
	}
	
	public void handleAction(NSMenuItem item) {
		if (item == this.statusItem) {
//			Desktop.openURL(new URL("http://localhost:6780/?status"));
			try {
				MRJFileUtils.openURL("http://localhost:6780/?status");
			} catch (IOException e) {
				this.logWindow.setVisible(true);
				System.out.println("RingsStatus: Unable to use MRJ toolkit to open browser.");
			}
		} else if (item == this.terminalItem) {
			
		} else if (item == this.logItem) {
			this.logWindow.setVisible(true);
		}
	}
	
	public void write(int b) {
		if (b == -1) {
			this.b = -1;
			return;
		} else if (this.b == -1) {
			this.b = (byte) b;
		} else {
//			this.log.append(new String(new byte[] {this.b, (byte) b}));
			this.log.setStringValue(this.log.stringValue() + new String(new byte[] {this.b, (byte) b}));
			this.b = -1;
		}
	}
}
