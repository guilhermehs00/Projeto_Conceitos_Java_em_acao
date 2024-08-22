Sistema de Coleta e Análise de Dados de GPS com Consulta a Regiões


Este projeto é uma aplicação Android desenvolvida para coletar dados de GPS e realizar consultas de regiões geográficas com base em coordenadas de latitude e longitude. A aplicação foi projetada para operar em tempo real, gerenciando múltiplas threads para diferentes tarefas, como coleta de dados, consultas de regiões em fila e banco de dados, e processamento criptográfico dos resultados.

Funcionalidades Principais
Coleta de Dados GPS: A aplicação coleta coordenadas de GPS em intervalos regulares, armazenando latitude e longitude para uso em outras tarefas.

Consulta a Regiões na Fila: Verifica se existem regiões dentro de um raio especificado na fila de regiões monitoradas, retornando os resultados para processamento.

Consulta ao Banco de Dados: Pesquisa no banco de dados remoto por regiões dentro do raio especificado e descriptografa os dados para determinar proximidade.

Criptografia e Adição na Fila: Após processar os resultados das consultas, as regiões são criptografadas e adicionadas à fila de monitoramento para futuras consultas.

Multithreading: Utiliza várias threads para realizar operações de consulta e processamento de forma paralela, maximizando a eficiência e minimizando o tempo de resposta.

Arquitetura
A aplicação é dividida em várias threads que realizam tarefas específicas:

LerDadosGps: Responsável pela coleta contínua de dados de GPS.
ConsultarFila: Verifica regiões em uma fila local para determinar a proximidade com as coordenadas atuais.
ConsultarBD: Consulta o banco de dados para verificar a proximidade de regiões armazenadas remotamente.
CriptografarDados: Processa e criptografa os dados antes de adicioná-los à fila de regiões.
AdicionarFila: Adiciona as regiões processadas e criptografadas à fila para monitoramento contínuo.


Tecnologias Utilizadas
Java: Linguagem principal de desenvolvimento.
Android SDK: Para desenvolvimento da aplicação Android.
Firebase Firestore: Para armazenamento e consulta de dados em tempo real.
Multithreading: Para gerenciamento eficiente de tarefas paralelas.

Contribuições são bem-vindas! Sinta-se à vontade para abrir um issue ou enviar um pull request.
