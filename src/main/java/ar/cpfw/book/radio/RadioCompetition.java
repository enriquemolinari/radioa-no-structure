package ar.cpfw.book.radio;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;

public class RadioCompetition {

	private JPanel contentPane;
	private JLabel lblName;
	private JTextField txtName;
	private JLabel lblLastName;
	private JTextField txtLastName;
	private JLabel lblId;
	private JTextField txtId;
	private JLabel lblPhone;
	private JTextField txtPhone;
	private JLabel lblEmail;
	private JTextField txtEmail;
	private JComboBox<Item> comboBox;
	private JButton btnOk;
	private JLabel lblCompetition;

	public RadioCompetition() throws SQLException {
		var frame = new JFrame("Inscription to Competition");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBounds(100, 100, 451, 229);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		frame.setContentPane(contentPane);

		formElements();
		layout();
		frame.setVisible(true);
	}

	private Connection connection() throws SQLException {
		String url = "jdbc:derby://localhost:1527/radiocompetition";
		String user = "app";
		String password = "app";
		return DriverManager.getConnection(url, user, password);
	}

	private void formElements() throws SQLException {
		lblName = new JLabel("Name:");

		txtName = new JTextField();
		txtName.setColumns(10);

		lblLastName = new JLabel("Last name:");

		txtLastName = new JTextField();
		txtLastName.setColumns(10);

		lblId = new JLabel("Id:");

		txtId = new JTextField();
		txtId.setColumns(10);

		lblPhone = new JLabel("Phone:");

		txtPhone = new JTextField();
		txtPhone.setColumns(10);

		lblEmail = new JLabel("Email:");

		txtEmail = new JTextField();
		txtEmail.setColumns(10);

		btnOk = new JButton("Ok");

		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				var worker = new SwingWorker<Void, Void>() {
					@Override
					protected void done() {
						try {
							get();
							JOptionPane.showMessageDialog(contentPane, "Inscription done successfully !");
						} catch (InterruptedException | ExecutionException e) {
							JOptionPane.showMessageDialog(contentPane, "Something went wrong...");
							throw new RuntimeException(e);
						}
						btnOk.setEnabled(true);
					}

					@Override
					protected Void doInBackground() throws Exception {
						btnOk.setEnabled(false);
						saveInscription();
						// to make the compiler happy...
						return null;
					}
				};
				
				if (validations())
					worker.execute();
			}
		});

		lblCompetition = new JLabel("Competition:");
		allCompetitions();
	}

	private boolean validations() {
		if ("".equals(txtName.getText())) {
			JOptionPane.showMessageDialog(this.contentPane, "Name cannot be empty");
			return false;
		}

		if ("".equals(txtLastName.getText())) {
			JOptionPane.showMessageDialog(this.contentPane, "Last name cannot be empty");
			return false;
		}

		if ("".equals(txtId.getText())) {
			JOptionPane.showMessageDialog(this.contentPane, "Id cannot be empty");
			return false;
		}

		if (!checkEmail(txtEmail.getText())) {
			JOptionPane.showMessageDialog(this.contentPane, "email cannot be empty");
			return false;
		}

		if (!checkPhone(txtPhone.getText())) {
			JOptionPane.showMessageDialog(this.contentPane,
					"El teléfono debe ingresarse de la siguiente forma: NNNN-NNNNNN");
			return false;
		}
		if(this.comboBox.getSelectedIndex() == 0) {
			JOptionPane.showMessageDialog(this.contentPane,
					"Choose a competition");
			return false;
		}
		
		return true;
	}
	
	private void allCompetitions() throws SQLException {
		Connection c = connection();
		try {
			PreparedStatement st = c.prepareStatement(
					"select id, description from competition "
					+ "where inscription_start_date <= ? and inscription_end_date >= ?");

			st.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
			st.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));

			ResultSet resultSet = st.executeQuery();

			var competitions = new ArrayList<Item>();
			competitions.add(new Item(0, "Select One"));
			while (resultSet.next()) {
				competitions.add(new Item(resultSet.getInt("id"), resultSet.getString("description")));
			}

			this.comboBox = new JComboBox<Item>(
					new CompetitionComboBoxModel(competitions.toArray(new Item[competitions.size()])));

		} finally {
			c.close();
		}
	}

	private void saveInscription() throws SQLException {
		Connection c = connection();
		try {
			c.setAutoCommit(false);
			PreparedStatement st = c
					.prepareStatement("insert into competitor(first_name, last_name, person_id, email, phone, points) "
							+ "values(?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);

			st.setString(1, txtName.getText());
			st.setString(2, txtLastName.getText());
			st.setString(3, txtId.getText());
			st.setString(4, txtEmail.getText());
			st.setString(5, txtPhone.getText());
			st.setInt(6, 0);
			st.executeUpdate();

			ResultSet generatedKeys = st.getGeneratedKeys();
			generatedKeys.next();

			PreparedStatement st2 = c.prepareStatement(
					"insert into inscription(id_competition, id_competitor, inscription_date) " + "values(?,?,?)");

			st2.setInt(1, ((Item) this.comboBox.getSelectedItem()).id());
			st2.setInt(2, generatedKeys.getInt(1));
			st2.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
			st2.executeUpdate();

			c.commit();
		} catch (Exception e) {
			try {
				c.rollback();
				throw new RuntimeException(e);
			} catch (SQLException s) {
				throw new RuntimeException(s);
			}
		} finally {
			try {
				c.setAutoCommit(true);
			} catch (SQLException s) {
				throw new RuntimeException(s);
			}
		}
	}

	private boolean checkEmail(String email) {
		String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
		return email.matches(regex);
	}

	private boolean checkPhone(String telefono) {
		String regex = "\\d{4}-\\d{6}";
		return telefono.matches(regex);
	}

	private void layout() {
		GroupLayout gl_contentPane = new GroupLayout(contentPane);
		gl_contentPane.setHorizontalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING).addGroup(gl_contentPane
				.createSequentialGroup().addContainerGap()
				.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING).addGroup(gl_contentPane
						.createSequentialGroup()
						.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING).addComponent(lblLastName)
								.addComponent(lblId).addComponent(lblPhone).addComponent(lblEmail).addComponent(lblName)
								.addComponent(lblCompetition))
						.addPreferredGap(ComponentPlacement.RELATED, 28, Short.MAX_VALUE)
						.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING, false)
								.addComponent(comboBox, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(txtEmail, Alignment.TRAILING).addComponent(txtPhone, Alignment.TRAILING)
								.addComponent(txtId, Alignment.TRAILING).addComponent(txtLastName, Alignment.TRAILING)
								.addComponent(txtName, Alignment.TRAILING, GroupLayout.DEFAULT_SIZE, 298,
										Short.MAX_VALUE)))
						.addComponent(btnOk, Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, 86,
								GroupLayout.PREFERRED_SIZE))
				.addContainerGap()));
		gl_contentPane.setVerticalGroup(gl_contentPane.createParallelGroup(Alignment.LEADING).addGroup(gl_contentPane
				.createSequentialGroup()
				.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
						.addComponent(txtName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
								GroupLayout.PREFERRED_SIZE)
						.addComponent(lblName))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE).addComponent(lblLastName).addComponent(
						txtLastName, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_contentPane.createParallelGroup(Alignment.TRAILING).addComponent(lblId).addComponent(txtId,
						GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(ComponentPlacement.RELATED)
				.addGroup(gl_contentPane.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_contentPane.createSequentialGroup().addComponent(lblPhone)
								.addPreferredGap(ComponentPlacement.UNRELATED).addComponent(lblEmail))
						.addGroup(gl_contentPane.createSequentialGroup()
								.addComponent(txtPhone, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addComponent(txtEmail, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
										GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(ComponentPlacement.RELATED)
								.addGroup(gl_contentPane.createParallelGroup(Alignment.BASELINE)
										.addComponent(comboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
												GroupLayout.PREFERRED_SIZE)
										.addComponent(lblCompetition))))
				.addPreferredGap(ComponentPlacement.RELATED).addComponent(btnOk).addContainerGap(67, Short.MAX_VALUE)));
		contentPane.setLayout(gl_contentPane);
	}
}
