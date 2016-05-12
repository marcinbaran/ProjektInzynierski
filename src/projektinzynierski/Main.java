package projektinzynierski;


import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import java.io.*;
import java.net.*;
import javax.imageio.*;

public class Main extends JFrame {

    BufferedImage image;
    JLabel promptLabel;
    JTextField prompt;
    JButton promptButton;
    JFileChooser fileChooser;
    JButton loadButton;
    JButton toGrayscaleButton;
    JButton processingButton;
    JScrollPane scrollPane;
    JLabel imgLabel;

    public Main() {
        super("Image processing");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        Container contentPane = getContentPane();
        JPanel inputPanel = new JPanel();
        promptLabel = new JLabel("Filename:");
        inputPanel.add(promptLabel);
        prompt = new JTextField(20);
        inputPanel.add(prompt);
        promptButton = new JButton("Browse");
        inputPanel.add(promptButton);
        contentPane.add(inputPanel, BorderLayout.NORTH);
        fileChooser = new JFileChooser();
        promptButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        int returnValue
                        = fileChooser.showOpenDialog(null);
                        if (returnValue
                        == JFileChooser.APPROVE_OPTION) {
                            File selectedFile
                            = fileChooser.getSelectedFile();
                            if (selectedFile != null) {
                                prompt.setText(selectedFile.getAbsolutePath());
                            }
                        }
                    }
                }
        );

        imgLabel = new JLabel();
        scrollPane = new JScrollPane(imgLabel);
        scrollPane.setPreferredSize(new Dimension(700, 500));
        contentPane.add(scrollPane, BorderLayout.CENTER);

        JPanel outputPanel = new JPanel();
        loadButton = new JButton("Load");
        outputPanel.add(loadButton);
        loadButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            String name = prompt.getText();
                            File file = new File(name);
                            if (file.exists()) {
                                image = ImageIO.read(file.toURL());
                                if (image == null) {
                                    System.err.println("Invalid input file format");
                                } else {
                                    imgLabel.setIcon(new ImageIcon(image));
                                }
                            } else {
                                System.err.println("Bad filename");
                            }
                        } catch (MalformedURLException mur) {
                            System.err.println("Bad filename");
                        } catch (IOException ioe) {
                            System.err.println("Error reading file");
                        }
                    }
                }
        );

        toGrayscaleButton = new JButton("Grayscale");
        outputPanel.add(toGrayscaleButton);
        toGrayscaleButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        toGrayscale(image);
                        imgLabel.setIcon(new ImageIcon(image));
                    }
                });

        processingButton = new JButton("Processing");
        outputPanel.add(processingButton);
        processingButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        Processing(image);
                        imgLabel.setIcon(new ImageIcon(image));
                    }
                });

        contentPane.add(outputPanel, BorderLayout.SOUTH);
    }

    private static void Processing(BufferedImage img) {
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        double tabR[] = new double[256];
        double tabG[] = new double[256];
        double tabB[] = new double[256];
        double tabA[] = new double[256];

        for (int i = 0; i < tabA.length; i++) {
            tabA[i] = 0;
            tabR[i] = 0;
            tabG[i] = 0;
            tabB[i] = 0;
        }

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int rgb = img.getRGB(x, y);
                tabA[(rgb & 0xff000000) >>> 24]++;
                tabR[(rgb & 0x00ff0000) >>> 16]++;
                tabG[(rgb & 0x0000ff00) >>> 8]++;
                tabB[(rgb & 0x000000ff)]++;
            }
        }

        int numberOfPixels = w * h;

        double DSumR[] = new double[256];
        double DSumG[] = new double[256];
        double DSumB[] = new double[256];
        double DSumA[] = new double[256];

        double SumR = 0;
        double SumG = 0;
        double SumB = 0;
        double SumA = 0;
        for (int i = 0; i < 256; i++) {
            SumR += (tabR[i] / numberOfPixels);
            SumG += (tabG[i] / numberOfPixels);
            SumB += (tabB[i] / numberOfPixels);
            SumA += (tabA[i] / numberOfPixels);

            DSumA[i] += SumA;
            DSumR[i] += SumR;
            DSumG[i] += SumG;
            DSumB[i] += SumB;
        }

        double LUTr[] = new double[256];
        double LUTg[] = new double[256];
        double LUTb[] = new double[256];
        double LUTa[] = new double[256];
        UpdateLUT(DSumR, LUTr);
        UpdateLUT(DSumG, LUTg);
        UpdateLUT(DSumB, LUTb);
        UpdateLUT(DSumA, LUTa);

        for (int i = 0; i < tabA.length; i++) {
            tabA[i] = 0;
            tabR[i] = 0;
            tabG[i] = 0;
            tabB[i] = 0;
        }

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int rgb = img.getRGB(x, y);
                int a = (rgb & 0xff000000) >>> 24;
                int r = (rgb & 0x00ff0000) >>> 16;
                int g = (rgb & 0x0000ff00) >>> 8;
                int b = rgb & 0x000000ff;

                r = (int) (LUTr[r]);
                g = (int) (LUTg[g]);
                b = (int) (LUTb[b]);
                a = (int) (LUTa[a]);
                System.out.println(r+"\t"+g+"\t"+b+"\t"+a+"\t");
                rgb = b | (g << 8) | (r << 16) | (a << 24);
             
                img.setRGB(x, y, rgb);
            }
        }
    }

    private static void toGrayscale(BufferedImage img) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        ColorConvertOp op = new ColorConvertOp(cs, null);
        op.filter(img, img);

    }

    private static void UpdateLUT(double[] tab, double[] LUT) {
        int i = 0;
        double D0min;

        while (tab[i] == 0) {
            i++;
        }
        D0min = tab[i];

        for (int j = 0; j < 256; j++) {
            LUT[j] = (((tab[j] - D0min) / (1 - D0min)) * (256 - 1));

        }

    }

    public static void main(String args[]) {
        JFrame frame = new Main();
        frame.pack();
        frame.show();
    }
}