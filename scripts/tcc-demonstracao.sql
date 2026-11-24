-- Base descartável para o TCC. Toda a massa abaixo é sintética.
-- Importe apenas em uma instância de ensaio, via cliente em lote sem --force.
-- CREATE DATABASE falha se o nome já existir. Use uma instância vazia.
CREATE DATABASE db_watt_vision CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE db_watt_vision;

-- =====================================================================
-- PARTE 1 — DDL (ESTRUTURA DAS TABELAS)
-- =====================================================================

CREATE TABLE tbl_empresa (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    razao_social   VARCHAR(150) NOT NULL,
    nome_fantasia  VARCHAR(150),
    cnpj           VARCHAR(18)  NOT NULL UNIQUE,
    email          VARCHAR(100),
    telefone       VARCHAR(20),
    status         VARCHAR(20)  NOT NULL DEFAULT 'Ativa',
    CONSTRAINT chk_empresa_status CHECK (status IN ('Ativa', 'Inativa'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_local (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa   INT NOT NULL,
    codigo       VARCHAR(20)  NOT NULL UNIQUE,
    nome         VARCHAR(100) NOT NULL,
    endereco     VARCHAR(255),
    responsavel  VARCHAR(100),
    descricao    VARCHAR(255),
    status       VARCHAR(20)  NOT NULL DEFAULT 'Ativo',
    CONSTRAINT fk_local_empresa FOREIGN KEY (id_empresa) REFERENCES tbl_empresa(id),
    CONSTRAINT chk_local_status CHECK (status IN ('Ativo', 'Inativo'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_setor (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    id_local     INT NOT NULL,
    codigo       VARCHAR(20)  NOT NULL UNIQUE,
    nome         VARCHAR(100) NOT NULL,
    responsavel  VARCHAR(100),
    descricao    VARCHAR(255),
    status       VARCHAR(20)  NOT NULL DEFAULT 'Ativo',
    CONSTRAINT fk_setor_local FOREIGN KEY (id_local) REFERENCES tbl_local(id),
    CONSTRAINT chk_setor_status CHECK (status IN ('Ativo', 'Inativo'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_equipamento (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    id_setor          INT NOT NULL,
    codigo            VARCHAR(20)  NOT NULL UNIQUE,
    nome              VARCHAR(100) NOT NULL,
    tipo_equipamento  VARCHAR(100),
    fabricante        VARCHAR(100),
    modelo            VARCHAR(100),
    numero_serie      VARCHAR(50),
    descricao         VARCHAR(255),
    potencia_nominal  DECIMAL(10,2),
    tensao_nominal    DECIMAL(10,2),
    data_instalacao   DATE,
    status            VARCHAR(20)  NOT NULL DEFAULT 'Ativo',
    CONSTRAINT fk_equipamento_setor FOREIGN KEY (id_setor) REFERENCES tbl_setor(id),
    CONSTRAINT chk_equipamento_status CHECK (status IN ('Ativo', 'Inativo'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_usuario (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa     INT NOT NULL,
    nome           VARCHAR(100) NOT NULL,
    email          VARCHAR(100) NOT NULL UNIQUE,
    senha          VARCHAR(255) NOT NULL,
    perfil         VARCHAR(20)  NOT NULL DEFAULT 'Gestor',
    status         VARCHAR(20)  NOT NULL DEFAULT 'Ativo',
    data_cadastro  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultimo_acesso  DATETIME NULL,
    CONSTRAINT fk_usuario_empresa FOREIGN KEY (id_empresa) REFERENCES tbl_empresa(id),
    CONSTRAINT chk_usuario_perfil CHECK (perfil IN ('Administrador', 'Gestor')),
    CONSTRAINT chk_usuario_status CHECK (status IN ('Ativo', 'Inativo'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_usuario_local (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario  INT NOT NULL,
    id_local    INT NOT NULL,
    CONSTRAINT fk_ul_usuario FOREIGN KEY (id_usuario) REFERENCES tbl_usuario(id) ON DELETE CASCADE,
    CONSTRAINT fk_ul_local   FOREIGN KEY (id_local)   REFERENCES tbl_local(id)   ON DELETE CASCADE,
    CONSTRAINT uq_usuario_local UNIQUE (id_usuario, id_local)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_dispositivo_iot (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa        INT NOT NULL,
    nome              VARCHAR(100) NOT NULL,
    identificador     VARCHAR(50)  NOT NULL UNIQUE,
    mac_address       VARCHAR(17)  NOT NULL UNIQUE,
    modelo            VARCHAR(100),
    numero_serie      VARCHAR(50),
    ip                VARCHAR(45),
    versao_firmware   VARCHAR(20),
    data_instalacao   DATE,
    ssid              VARCHAR(100),
    broker            VARCHAR(100),
    port              INT,
    topic             VARCHAR(150),
    client_id         VARCHAR(100),
    tx_interval_sec   INT,
    wifi              BOOLEAN NOT NULL DEFAULT FALSE,
    mqtt              BOOLEAN NOT NULL DEFAULT FALSE,
    status            VARCHAR(20) NOT NULL DEFAULT 'offline',
    data_comunicacao  DATETIME,
    CONSTRAINT fk_disp_empresa FOREIGN KEY (id_empresa) REFERENCES tbl_empresa(id),
    CONSTRAINT chk_disp_status CHECK (status IN ('online', 'offline', 'instavel', 'disponivel')),
    INDEX idx_disp_status   (status),
    INDEX idx_disp_data_com (data_comunicacao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_ponto_medicao (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    nome                VARCHAR(100) NOT NULL,
    descricao           VARCHAR(150),
    data_instalacao     DATETIME NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'Ativo',
    id_local            INT NULL,
    id_setor            INT NULL,
    id_equipamento      INT NULL,
    id_dispositivo_iot  INT NOT NULL,
    CONSTRAINT fk_pm_local       FOREIGN KEY (id_local)           REFERENCES tbl_local(id),
    CONSTRAINT fk_pm_setor       FOREIGN KEY (id_setor)           REFERENCES tbl_setor(id),
    CONSTRAINT fk_pm_equipamento FOREIGN KEY (id_equipamento)     REFERENCES tbl_equipamento(id),
    CONSTRAINT fk_pm_dispositivo FOREIGN KEY (id_dispositivo_iot) REFERENCES tbl_dispositivo_iot(id),
    CONSTRAINT chk_pm_status CHECK (status IN ('Ativo', 'Inativo')),
    CONSTRAINT chk_pm_escopo CHECK (
        (CASE WHEN id_local       IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN id_setor       IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN id_equipamento IS NOT NULL THEN 1 ELSE 0 END) = 1
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_pzem (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    id_ponto_medicao  INT NOT NULL,
    numero_serie      VARCHAR(50) NOT NULL UNIQUE,
    modelo            VARCHAR(50) NOT NULL DEFAULT 'PZEM-004T',
    versao            VARCHAR(20),
    data_instalacao   DATE,
    status            VARCHAR(20) NOT NULL DEFAULT 'Ativo',
    CONSTRAINT fk_pzem_ponto FOREIGN KEY (id_ponto_medicao) REFERENCES tbl_ponto_medicao(id),
    CONSTRAINT chk_pzem_status CHECK (status IN ('Ativo', 'Inativo', 'Defeito')),
    INDEX idx_pzem_ponto (id_ponto_medicao)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_medicao (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_pzem            INT NOT NULL,
    id_ponto_medicao   INT NOT NULL,
    data_hora          DATETIME NOT NULL,
    tensao             DECIMAL(10,2),
    corrente           DECIMAL(10,3),
    potencia_ativa     DECIMAL(10,2),
    energia_acumulada  DECIMAL(12,3),
    frequencia         DECIMAL(5,2),
    fator_potencia     DECIMAL(5,3),
    CONSTRAINT fk_medicao_pzem  FOREIGN KEY (id_pzem)          REFERENCES tbl_pzem(id),
    CONSTRAINT fk_medicao_ponto FOREIGN KEY (id_ponto_medicao) REFERENCES tbl_ponto_medicao(id),
    CONSTRAINT uq_medicao_pzem_data UNIQUE (id_pzem, data_hora),
    INDEX idx_medicao_data       (data_hora),
    INDEX idx_medicao_ponto_data (id_ponto_medicao, data_hora),
    INDEX idx_medicao_pzem_data  (id_pzem, data_hora)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_tarifa (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    id_empresa    INT NOT NULL,
    descricao     VARCHAR(100) NOT NULL,
    valor_kwh     DECIMAL(10,4) NOT NULL,
    data_inicial  DATE NOT NULL,
    data_final    DATE NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'Ativa',
    CONSTRAINT fk_tarifa_empresa FOREIGN KEY (id_empresa) REFERENCES tbl_empresa(id),
    CONSTRAINT chk_tarifa_status  CHECK (status IN ('Ativa', 'Inativa')),
    CONSTRAINT chk_tarifa_periodo CHECK (data_final IS NULL OR data_final >= data_inicial),
    INDEX idx_tarifa_vigencia (id_empresa, data_inicial, data_final)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_config_comunicacao (
    id                              INT PRIMARY KEY,
    tempo_offline_minutos           INT NOT NULL DEFAULT 5,
    tempo_instavel_minutos          INT NOT NULL DEFAULT 2,
    tempo_reconexao_estavel_minutos INT NOT NULL DEFAULT 3,
    gerar_alarme_offline            BOOLEAN NOT NULL DEFAULT TRUE,
    gerar_alarme_reconexao          BOOLEAN NOT NULL DEFAULT TRUE,
    registrar_historico             BOOLEAN NOT NULL DEFAULT TRUE,
    intervalo_verificacao_minutos   INT NOT NULL DEFAULT 1,
    CONSTRAINT chk_config_id CHECK (id = 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_config_sistema (
    id              INT PRIMARY KEY,
    nome_empresa    VARCHAR(150) NOT NULL DEFAULT 'Watt Vision',
    fuso_horario    VARCHAR(50)  NOT NULL DEFAULT 'America/Sao_Paulo',
    formato_data    VARCHAR(20)  NOT NULL DEFAULT 'DD/MM/YYYY',
    tema            VARCHAR(20)  NOT NULL DEFAULT 'Escuro',
    unidade_energia VARCHAR(10)  NOT NULL DEFAULT 'kWh',
    CONSTRAINT chk_config_sistema_id CHECK (id = 1),
    CONSTRAINT chk_config_sistema_formato CHECK (
        formato_data IN ('DD/MM/YYYY', 'MM/DD/YYYY', 'YYYY-MM-DD')
    ),
    CONSTRAINT chk_config_sistema_tema CHECK (tema IN ('Escuro', 'Claro'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_tipo_alerta (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    nome       VARCHAR(50) NOT NULL UNIQUE,
    categoria  VARCHAR(20) NOT NULL,
    descricao  VARCHAR(255),
    CONSTRAINT chk_tipo_alerta_categoria CHECK (categoria IN ('energetico', 'comunicacao'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_limite (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    descricao        VARCHAR(150),
    grandeza         VARCHAR(20) NOT NULL,
    operador         VARCHAR(2)  NOT NULL,
    valor            DECIMAL(10,3) NOT NULL,
    severidade       VARCHAR(20) NOT NULL,
    periodo_inicial  DATE NULL,
    periodo_final    DATE NULL,
    ativa            BOOLEAN NOT NULL DEFAULT TRUE,
    id_local         INT NULL,
    id_setor         INT NULL,
    id_equipamento   INT NULL,
    CONSTRAINT fk_limite_local       FOREIGN KEY (id_local)       REFERENCES tbl_local(id),
    CONSTRAINT fk_limite_setor       FOREIGN KEY (id_setor)       REFERENCES tbl_setor(id),
    CONSTRAINT fk_limite_equipamento FOREIGN KEY (id_equipamento) REFERENCES tbl_equipamento(id),
    CONSTRAINT chk_limite_grandeza   CHECK (grandeza IN ('tensao', 'corrente', 'potencia', 'energia')),
    CONSTRAINT chk_limite_operador   CHECK (operador IN ('>', '<', '>=', '<=')),
    CONSTRAINT chk_limite_severidade CHECK (severidade IN ('meta', 'informativo', 'atencao', 'critico')),
    CONSTRAINT chk_limite_escopo CHECK (
        (CASE WHEN id_local       IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN id_setor       IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN id_equipamento IS NOT NULL THEN 1 ELSE 0 END) = 1
    ),
    CONSTRAINT chk_limite_periodo CHECK (
        periodo_final IS NULL OR periodo_inicial IS NULL OR periodo_final >= periodo_inicial
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_alerta (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_tipo_alerta       INT NOT NULL,
    id_limite            INT NULL,
    id_dispositivo_iot   INT NULL,
    id_medicao           BIGINT NULL,
    valor_detectado      DECIMAL(12,3),
    valor_limite         DECIMAL(12,3),
    severidade           VARCHAR(20) NOT NULL,
    mensagem             VARCHAR(255),
    data_hora            DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status               VARCHAR(20) NOT NULL DEFAULT 'ativo',
    reconhecido_por      INT NULL,
    data_reconhecimento  DATETIME NULL,
    CONSTRAINT fk_alerta_tipo        FOREIGN KEY (id_tipo_alerta)     REFERENCES tbl_tipo_alerta(id),
    CONSTRAINT fk_alerta_limite      FOREIGN KEY (id_limite)          REFERENCES tbl_limite(id),
    CONSTRAINT fk_alerta_dispositivo FOREIGN KEY (id_dispositivo_iot) REFERENCES tbl_dispositivo_iot(id),
    CONSTRAINT fk_alerta_medicao     FOREIGN KEY (id_medicao)         REFERENCES tbl_medicao(id),
    CONSTRAINT fk_alerta_usuario     FOREIGN KEY (reconhecido_por)    REFERENCES tbl_usuario(id),
    CONSTRAINT chk_alerta_severidade CHECK (severidade IN ('informativo', 'atencao', 'critico')),
    CONSTRAINT chk_alerta_status     CHECK (status IN ('ativo', 'reconhecido')),
    INDEX idx_alerta_data   (data_hora),
    INDEX idx_alerta_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_evento_conexao (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_dispositivo_iot  INT NOT NULL,
    evento              VARCHAR(20) NOT NULL,
    data_hora           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    duracao_segundos    INT NULL,
    CONSTRAINT fk_evento_disp FOREIGN KEY (id_dispositivo_iot) REFERENCES tbl_dispositivo_iot(id) ON DELETE CASCADE,
    CONSTRAINT chk_evento_tipo CHECK (evento IN ('desconectado', 'reconectado')),
    INDEX idx_evento_disp_data (id_dispositivo_iot, data_hora),
    INDEX idx_evento_evento    (evento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_relatorio (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    id_usuario    INT NOT NULL,
    titulo        VARCHAR(150) NOT NULL,
    tipo          VARCHAR(20)  NOT NULL,
    data_geracao  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    parametros    JSON,
    CONSTRAINT fk_relatorio_usuario FOREIGN KEY (id_usuario) REFERENCES tbl_usuario(id),
    CONSTRAINT chk_relatorio_tipo CHECK (tipo IN ('historico', 'comparacao', 'previsibilidade'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tbl_arquivo_relatorio (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    id_relatorio   INT NOT NULL,
    nome_arquivo   VARCHAR(150) NOT NULL,
    formato        VARCHAR(10)  NOT NULL,
    caminho        VARCHAR(255) NOT NULL,
    tamanho_bytes  INT,
    data_geracao   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_arquivo_relatorio FOREIGN KEY (id_relatorio) REFERENCES tbl_relatorio(id) ON DELETE CASCADE,
    CONSTRAINT chk_arquivo_formato CHECK (formato IN ('PDF', 'CSV'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Dados fictícios. A conta é provisionada depois, com senha escolhida localmente.
START TRANSACTION;

INSERT INTO tbl_empresa (id, razao_social, nome_fantasia, cnpj)
VALUES (1, 'Watt Vision TCC - Empresa Fictícia', 'Watt Vision TCC', '00.000.000/0000-00');

INSERT INTO tbl_local (id, id_empresa, codigo, nome)
VALUES (1, 1, 'TCC-LOC-01', 'Planta de demonstração');

INSERT INTO tbl_setor (id, id_local, codigo, nome)
VALUES (1, 1, 'TCC-SET-01', 'Usinagem de demonstração');

INSERT INTO tbl_equipamento (id, id_setor, codigo, nome, potencia_nominal, tensao_nominal)
VALUES (1, 1, 'TCC-EQP-01', 'Motor Elétrico WEG 01', 2200.00, 220.00);

INSERT INTO tbl_dispositivo_iot
    (id, id_empresa, nome, identificador, mac_address, modelo, mqtt, status, data_comunicacao)
VALUES (1, 1, 'ESP32 de demonstração', 'TCC-ESP32-01', '02:00:00:00:00:01', 'ESP32',
        TRUE, 'online', NOW() - INTERVAL 5 MINUTE);

INSERT INTO tbl_ponto_medicao (id, nome, id_equipamento, id_dispositivo_iot)
VALUES (1, 'Ponto Motor WEG 01', 1, 1);

INSERT INTO tbl_pzem (id, id_ponto_medicao, numero_serie)
VALUES (1, 1, 'TCC-PZEM-01');

INSERT INTO tbl_medicao
    (id_pzem, id_ponto_medicao, data_hora, tensao, corrente, potencia_ativa,
     energia_acumulada, frequencia, fator_potencia)
VALUES
    (1, 1, NOW() - INTERVAL 45 MINUTE, 219.70, 9.100, 1940.00, 14.050, 60.01, 0.960),
    (1, 1, NOW() - INTERVAL 30 MINUTE, 220.40, 9.530, 2020.00, 14.550, 59.99, 0.964),
    (1, 1, NOW() - INTERVAL 15 MINUTE, 220.90, 9.640, 2080.00, 15.060, 60.00, 0.969),
    (1, 1, NOW() - INTERVAL 5 MINUTE, 221.10, 9.800, 2100.00, 15.400, 60.00, 0.970);

INSERT INTO tbl_tarifa (id_empresa, descricao, valor_kwh, data_inicial)
VALUES (1, 'Tarifa de demonstração', 0.8000, CURRENT_DATE());

INSERT INTO tbl_tipo_alerta (id, nome, categoria)
VALUES (1, 'Alerta energético de demonstração', 'energetico');

INSERT INTO tbl_alerta (id_tipo_alerta, id_dispositivo_iot, severidade, mensagem)
VALUES (1, 1, 'informativo', 'Ocorrência sintética para apresentação');

INSERT INTO tbl_config_comunicacao (id) VALUES (1);
INSERT INTO tbl_config_sistema (id, nome_empresa) VALUES (1, 'Watt Vision TCC');

COMMIT;
