package com.owens.oobjloader.lwjgl;

import org.lwjgl.Sys;

import javax.swing.*;

import java.awt.BorderLayout;

import javax.swing.filechooser.FileNameExtensionFilter;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

public class Controller extends JFrame{
	
	
	public String fileLocation = "";
	JLabel lblVertixes;
	JLabel lblDistance;
	private JButton btnOpen;
	public void setDistance(double distance){
		lblDistance.setText("Distance: " + Math.round(distance * 1000D) / 1000D + "cm");
		this.revalidate();
		this.repaint();
		
	}
	
	public void setVCount(int number){
		lblVertixes.setText("Vertices: "+ number);
		this.revalidate();
		this.repaint();
		
	}

	public void refresh() {
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
		
		final JLabel lblNotCalibrated = new JLabel("Not Calibrated");
		lblNotCalibrated.setBounds(132, 298, 150, 23);
		getContentPane().add(lblNotCalibrated);

		final JTextField calibrateField = new JTextField("0");
		calibrateField.setBounds(155, 265, 89, 23);
		getContentPane().add(calibrateField);

		JButton btnCalibrate = new JButton("Calibrate");
		btnCalibrate.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(!DisplayTest.isRunning) {
					Sys.alert("Betoni Demo", "Open the model first.");
					return;
				}

				if (DisplayTest.spheres.size() < 2) {
					Sys.alert("Betoni Demo", "Place two points on model to calibrate.");
					return;
				}

				double calibrated;
				try {
					calibrated = Double.parseDouble(calibrateField.getText());
				} catch (Exception e) {
					Sys.alert("Betoni Demo", "Please enter the correct number of centimeters");
					return;
				}

				double distance = DisplayTest.sqrDistPP3D(
						DisplayTest.sphereCoords.get(0),
						DisplayTest.sphereCoords.get(1)
				);

				DisplayTest.ratio = calibrated / distance;
				lblNotCalibrated.setText("cm/unit = " + Math.round(DisplayTest.ratio * 1000D) / 1000D);
				setDistance(DisplayTest.ratio * distance);
				refresh();
			}
		});
		btnCalibrate.setBounds(10, 298, 89, 23);
		getContentPane().add(btnCalibrate);
		
		lblVertixes = new JLabel("Vertices");
		lblVertixes.setBounds(10, 84, 234, 23);
		getContentPane().add(lblVertixes);
		
		btnOpen = new JButton("Open");
		btnOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {

			    JFileChooser chooser = new JFileChooser();
			    FileNameExtensionFilter filter = new FileNameExtensionFilter("OBJ files", "obj");
			    chooser.setFileFilter(filter);
			    int returnVal = chooser.showOpenDialog(chooser);
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
					System.out.println("You chose to open this file: " +
							chooser.getSelectedFile().getAbsolutePath());
					fileLocation = chooser.getSelectedFile().getAbsolutePath();
					Runnable run = new Runnable() {
						@Override
						public void run() {
							try {
								btnOpen.setVisible(false);
								refresh();
								DisplayTest.init(false);
								DisplayTest.run(fileLocation, null);

							} catch (Exception e) {
								e.printStackTrace(System.err);
								Sys.alert("Betoni Demo", "An error occured and the program will exit.");
							} finally {
								DisplayTest.cleanup();
								btnOpen.setVisible(true);
								refresh();
							}
						}
					};
					new Thread(run).start();
				}
			}
		});
		btnOpen.setBounds(10, 264, 89, 23);
		getContentPane().add(btnOpen);
	}
}
