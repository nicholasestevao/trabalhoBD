
package sistemaplanetario;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ResourceBundle;
import javafx.animation.AnimationTimer;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;
import javafx.scene.control.Alert;


/**
 * Controlador responsável pela interface de cadastro de planetas.
 * Implementa a interface Initializable para inicialização do controlador.
 */
public class CadastrarPlanetasController implements Initializable {

    /**
     * Botão utilizado para voltar à tela inicial.
     */
    @FXML
    private Button bVoltar;

    /**
     * Campo de texto para inserir o nome do planeta.
     */
    @FXML
    private TextField tfPlaneta;

    /**
     * Campo de texto para inserir o clima do planeta.
     */
    @FXML
    private TextField tfClima;

    /**
     * Campo de texto para inserir a pressão do planeta.
     */
    @FXML
    private TextField tfPressao;

    /**
     * Campo de texto para inserir a temperatura do planeta.
     */
    @FXML
    private TextField tfTemperatura;

    /**
     * Botão para cadastrar o planeta.
     */
    @FXML
    private Button bCadastrar;

    /**
     * ComboBox para selecionar o sistema planetário ao qual o planeta pertence.
     */
    @FXML
    private ComboBox<SistemaPlanetario> cbSistema;

    /**
     * Objeto de conexão com o banco de dados.
     */
    private Connection conexao;

    /**
     * Thread para obter a conexão com o banco de dados.
     */
    private AnimationTimer conexaoThread = new AnimationTimer() {
        /**
         * Método chamado periodicamente para tentar obter a conexão.
         */
        @Override
        public void handle(long now) {
            try {
                System.out.println("Obtendo conexão...");
                System.out.println(conexao);

                // Se não houver conexão, tenta obter do Stage (janela)
                if (conexao == null) {
                    Stage stage = (Stage) bVoltar.getScene().getWindow();
                    conexao = (Connection) stage.getUserData();

                    // Carrega os sistemas planetários no ComboBox
                    ResultSet resultSet = conexao.prepareStatement("SELECT NOME, GALAXIA FROM SISTEMA_PLANETARIO").executeQuery();
                    cbSistema.setVisibleRowCount(5);

                    while (resultSet.next()) {
                        SistemaPlanetario s = new SistemaPlanetario(resultSet.getString(1), resultSet.getString(2));
                        cbSistema.getItems().add(s);
                    }
                    resultSet.close();
                    this.stop();
                    verificaConexaoThread.start();
                }
            } catch (SQLException s) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Falha na conexão");
                alert.setContentText("Ocorreu uma falha na conexão");
                alert.showAndWait();

                System.out.println("ERRO: a conexão SQL apresentou erro - " + s.getMessage());
                conexao = null;
            }
        }
    };

    /**
     * Thread para verificar a validade da conexão.
     */
    private AnimationTimer verificaConexaoThread = new AnimationTimer() {
        /**
         * Método chamado periodicamente para verificar a validade da conexão.
         */
        @Override
        public void handle(long now) {
            try {
                if (!conexao.isValid(5000)) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Desconectado");
                    alert.setContentText("Você foi desconectado!");
                    alert.showAndWait();

                    Stage stage = (Stage) bVoltar.getScene().getWindow();
                    Parent root = FXMLLoader.load(getClass().getResource("Login.fxml"));
                    Scene scene = new Scene(root);
                    stage.setTitle("Login");
                    stage.setScene(scene);
                    stage.show();
                    this.stop();
                }
            } catch (SQLException s) {
                System.out.println(s.getSQLState());
            } catch (IOException e) {
                System.out.println("Não foi possível gerar a tela de Login.");
                e.printStackTrace();
            }
        }
    };

    /**
     * Método chamado ao clicar no botão "Cadastrar" para realizar o cadastro do planeta.
     */
    @FXML
    private void cadastrar(ActionEvent event) {
        try {
            SistemaPlanetario sistemaPlanetario = cbSistema.getValue();
            String sistema = "";
            String galaxia = "";
            if (sistemaPlanetario != null) {
                sistema = sistemaPlanetario.getNome();
                galaxia = sistemaPlanetario.getGalaxia();
            }

            String nome = tfPlaneta.getText();
            String clima = tfClima.getText();

            PreparedStatement obtemID = conexao.prepareStatement("SELECT MAX (ID) + 1 FROM PLANETA");
            ResultSet r = obtemID.executeQuery();
            int id = 1;
            while (r.next())
                id = r.getInt(1);
            r.close();

            PreparedStatement linha = conexao.prepareStatement("INSERT INTO PLANETA values (?,?,?,?,?,?,?)");
            linha.setInt(1, id);
            linha.setString(2, sistema);
            linha.setString(3, galaxia);
            linha.setString(4, nome);

            if (tfTemperatura.getText().isEmpty())
                linha.setNull(5, Types.FLOAT);
            else
                linha.setFloat(5, Float.parseFloat(tfTemperatura.getText()));

            if (tfPressao.getText().isEmpty())
                linha.setNull(6, Types.FLOAT);
            else
                linha.setFloat(6, Float.parseFloat(tfPressao.getText()));

            linha.setString(7, clima);
            linha.executeUpdate();

            conexao.commit();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Planeta cadastrado");
            alert.setHeaderText("Planeta cadastrado.");
            alert.setContentText("O planeta " + nome + " foi cadastrado!");
            alert.showAndWait();

            tfPlaneta.setText("");
            tfClima.setText("");
            tfPressao.setText("");
            tfTemperatura.setText("");
            cbSistema.setValue(null);
        } catch (SQLException s) {
            Alert alert = new Alert(Alert.AlertType.ERROR);

            System.out.println(s.getSQLState());
            System.out.println(s.getErrorCode());
            System.out.println(s.getMessage());

            String mensagem = "";
            String titulo = "";
            if (s.getErrorCode() == 1) {
                mensagem = "Esse planeta já está cadastrado no sistema.";
                titulo = "Planeta já cadastrado";
            } else if (s.getErrorCode() == 1400) {
                mensagem = "Você tentou inserir NULL num atributo NOT NULL.";
                titulo = "Atributo NULL";
            } else {
                mensagem = s.getMessage();
                titulo = "Erro ao cadastrar";
            }
            alert.setTitle(titulo);
            alert.setHeaderText(mensagem);
            alert.setContentText(s.getLocalizedMessage());
            alert.showAndWait();

            try {
                conexao.rollback();
            } catch (SQLException sq) {
                System.out.println("Rollback não executado!");
            }
        }
    }

    /**
     * Método chamado ao clicar no botão "Voltar" para retornar à tela inicial.
     */
    @FXML
    private void voltarInicio(ActionEvent event) {
        System.out.println("Voltar");
        try {
            Stage stage = (Stage) bVoltar.getScene().getWindow();
            Parent root = FXMLLoader.load(getClass().getResource("Inicio.fxml"));
            Scene scene = new Scene(root);
            stage.setTitle("Início");
            stage.setUserData(conexao);
            stage.setScene(scene);
            stage.show();
            stopThreads();
        } catch (IOException e) {
            System.out.println("Não foi possível abrir a tela de Inicio.");
        }
    }

    /**
     * Método para interromper as threads relacionadas à conexão.
     */
    private void stopThreads() {
        conexaoThread.stop();
        verificaConexaoThread.stop();
    }

    /**
     * Método chamado durante a inicialização do controlador.
     * Configura listeners nos campos de temperatura e pressão para aceitar apenas valores numéricos.
     *
     * @param url A localização usada para resolver caminhos relativos para o objeto raiz, ou null se a localização não é conhecida.
     * @param rb  O recurso usado para localizar o objeto raiz, ou null se o objeto raiz foi localizado.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        System.out.println("CADASTRAR PLANETAS CONTROLLER!");
        conexao = null;
        conexaoThread.start();

        tfTemperatura.textProperty().addListener(new ChangeListener<String>() {
            /**
             * Método chamado ao alterar o valor no campo de temperatura.
             */
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                                String newValue) {
                if (!newValue.matches("\\d*")) {
                    tfTemperatura.setText(newValue.replaceAll("[^\\d.]", ""));
                }
            }
        });

        tfPressao.textProperty().addListener(new ChangeListener<String>() {
            /**
             * Método chamado ao alterar o valor no campo de pressão.
             */
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue,
                                String newValue) {
                if (!newValue.matches("\\d*")) {
                    tfPressao.setText(newValue.replaceAll("[^\\d.]", ""));
                }
            }
        });
    }
}
