package org.newdawn.spaceinvaders;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class ButtonController {
    public void pressLoginBtn(LoginFrame loginFrame,JPanel panel, JFrame frame) {
        if (loginFrame.loginStatus) {
            JOptionPane.showMessageDialog(panel, "Already login");
        } else {
            loginFrame.startlogin();
            loginFrame.frame.addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent e) {
                    String tempId = loginFrame.user.Id;
                    JLabel userlabel = new JLabel(tempId);
                    userlabel.setLayout(null);
                    userlabel.setBounds(10, 10, 200, 30);
                    userlabel.setForeground(Color.white);
                    userlabel.setVisible(true);

                    panel.add(userlabel);
                    panel.revalidate();
                    panel.repaint();
                    frame.revalidate();
                    frame.repaint();
                }
            });
        }
    }
}
