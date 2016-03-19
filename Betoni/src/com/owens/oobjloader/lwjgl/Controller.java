package com.owens.oobjloader.lwjgl;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JButton;
import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

public class Controller extends JFrame{
	
	
	public String fileLocation = "";
	JLabel lblVertixes;
	JLabel lblDistance;
	private JButton btnOpen;
	public void setDistance(String text){
		lblDistance.setText(text);
		this.revalidate();
		this.repaint();
		
	}
	
	public void setVCount(int number){
		lblVertixes.setText("Vertices: "+number);
		this.revalidate();
		this.repaint();
		
	}
	
	public Controller() {
		
		try {
			UIManager.setLookAndFeel("com.jtattoo.plaf.smart.SmartLookAndFeel");
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setTitle("Controls");
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setAlwaysOnTop(true);
		getContentPane().setLayout(null);
		
		lblDistance = new JLabel("Distance");
		lblDistance.setBounds(10, 11, 234, 32);
		getContentPane().add(lblDistance);
		
		JButton btnCalibrate = new JButton("Calibrate");
		btnCalibrate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				
			}
		});
		btnCalibrate.setBounds(10, 298, 89, 23);
		getContentPane().add(btnCalibrate);
		
		JLabel lblNotCalibrated = new JLabel("Not Calibrated");
		lblNotCalibrated.setBounds(132, 298, 112, 23);
		getContentPane().add(lblNotCalibrated);
		
		lblVertixes = new JLabel("Vertices");
		lblVertixes.setBounds(10, 84, 234, 23);
		getContentPane().add(lblVertixes);
		
		btnOpen = new JButton("Open");
		btnOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
			    JFileChooser chooser = new JFileChooser();
			    FileNameExtensionFilter filter = new FileNameExtensionFilter(
			        "OBJ & MTL files", "obj", "mtl");
			    chooser.setFileFilter(filter);
			    int returnVal = chooser.showOpenDialog(chooser);
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			       System.out.println("You chose to open this file: " +
			            chooser.getSelectedFile().getAbsolutePath());
			       fileLocation = chooser.getSelectedFile().getAbsolutePath();
			       
			    }
			}
		});
		btnOpen.setBounds(10, 264, 89, 23);
		getContentPane().add(btnOpen);
	}
}
