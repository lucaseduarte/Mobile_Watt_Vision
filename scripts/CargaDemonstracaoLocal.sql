-- Execute uma vez no MySQL local, em modo lote sem --force.
-- Usa a empresa do administrador existente. Os codigos WVDEMO identificam a carga.
-- Se houver erro antes do COMMIT, encerre a sessao ou execute ROLLBACK.
USE db_watt_vision;
START TRANSACTION;

SET @demo_admin = (
    SELECT id FROM tbl_usuario
    WHERE email = 'admin@wattvision.com' AND perfil = 'Administrador' AND status = 'Ativo'
);
SET @demo_empresa = (SELECT id_empresa FROM tbl_usuario WHERE id = @demo_admin);
SET @demo_agora = NOW();
SET @demo_ontem = TIMESTAMP(CURRENT_DATE - INTERVAL 1 DAY, '12:00:00');

-- id_empresa e obrigatorio e codigo e unico. A carga falha na primeira
-- gravacao se o administrador estiver ausente ou se a carga ja existir.
INSERT INTO tbl_local (id_empresa, codigo, nome, endereco, responsavel, descricao, status)
VALUES (@demo_empresa, 'WVDEMO-LOCAL', 'DEMO - Planta Modelo', 'Rua de Teste, 100',
        'Equipe de demonstração', 'Instalação fictícia para o roteiro', 'Ativo');
SET @demo_local = LAST_INSERT_ID();

INSERT INTO tbl_setor (id_local, codigo, nome, responsavel, descricao, status)
VALUES (@demo_local, 'WVDEMO-USINAGEM', 'DEMO - Usinagem', 'Equipe A', 'Linha monitorada', 'Ativo'),
       (@demo_local, 'WVDEMO-UTIL', 'DEMO - Utilidades', 'Equipe B', 'Setor sem medições', 'Inativo');
SET @demo_setor = (SELECT id FROM tbl_setor WHERE codigo = 'WVDEMO-USINAGEM');
SET @demo_setor_vazio = (SELECT id FROM tbl_setor WHERE codigo = 'WVDEMO-UTIL');

INSERT INTO tbl_equipamento
(id_setor, codigo, nome, tipo_equipamento, fabricante, modelo, numero_serie, descricao,
 potencia_nominal, tensao_nominal, data_instalacao, status)
VALUES
(@demo_setor, 'WVDEMO-MOTOR', 'DEMO - Motor principal', 'Motor elétrico', 'Fabricante fictício',
 'Modelo 220', 'WVDEMO-M01', 'Motor com histórico de medições', 2200.00, 220.00, CURRENT_DATE - INTERVAL 30 DAY, 'Ativo'),
(@demo_setor_vazio, 'WVDEMO-BOMBA', 'DEMO - Bomba reserva', 'Bomba', 'Fabricante fictício',
 'Modelo 220', 'WVDEMO-B01', 'Equipamento sem leituras', 750.00, 220.00, CURRENT_DATE - INTERVAL 15 DAY, 'Inativo');
SET @demo_motor = (SELECT id FROM tbl_equipamento WHERE codigo = 'WVDEMO-MOTOR');
SET @demo_bomba = (SELECT id FROM tbl_equipamento WHERE codigo = 'WVDEMO-BOMBA');

INSERT INTO tbl_dispositivo_iot
(id_empresa, nome, identificador, mac_address, modelo, numero_serie, ip, versao_firmware,
 data_instalacao, ssid, broker, port, topic, client_id, tx_interval_sec, wifi, mqtt, status, data_comunicacao)
VALUES
(@demo_empresa, 'DEMO - ESP32 motor', 'WVDEMO-ESP01', '02:57:56:00:00:01', 'ESP32', 'WVDEMO-E01',
 '192.0.2.10', '1.0.0-demo', CURRENT_DATE - INTERVAL 30 DAY, 'WVDEMO-WIFI', 'mqtt.example.invalid',
 1883, 'wattvision/demo/motor', 'wv-demo-motor', 60, TRUE, TRUE, 'online', @demo_agora),
(@demo_empresa, 'DEMO - ESP32 reserva', 'WVDEMO-ESP02', '02:57:56:00:00:02', 'ESP32', 'WVDEMO-E02',
 '192.0.2.11', '1.0.0-demo', CURRENT_DATE - INTERVAL 15 DAY, 'WVDEMO-WIFI', 'mqtt.example.invalid',
 1883, 'wattvision/demo/reserva', 'wv-demo-reserva', 120, FALSE, FALSE, 'offline', @demo_agora - INTERVAL 20 MINUTE),
(@demo_empresa, 'DEMO - ESP32 instável', 'WVDEMO-ESP03', '02:57:56:00:00:03', 'ESP32', 'WVDEMO-E03',
 '192.0.2.12', '1.0.0-demo', CURRENT_DATE, 'WVDEMO-WIFI', 'mqtt.example.invalid',
 1883, 'wattvision/demo/instavel', 'wv-demo-instavel', 60, TRUE, FALSE, 'instavel', @demo_agora - INTERVAL 3 MINUTE),
(@demo_empresa, 'DEMO - ESP32 disponível', 'WVDEMO-ESP04', '02:57:56:00:00:04', 'ESP32', 'WVDEMO-E04',
 NULL, '1.0.0-demo', CURRENT_DATE, NULL, NULL, NULL, NULL, NULL, 60, FALSE, FALSE, 'disponivel', NULL);
SET @demo_esp1 = (SELECT id FROM tbl_dispositivo_iot WHERE identificador = 'WVDEMO-ESP01');
SET @demo_esp2 = (SELECT id FROM tbl_dispositivo_iot WHERE identificador = 'WVDEMO-ESP02');
SET @demo_esp3 = (SELECT id FROM tbl_dispositivo_iot WHERE identificador = 'WVDEMO-ESP03');

INSERT INTO tbl_ponto_medicao
(nome, descricao, data_instalacao, status, id_local, id_setor, id_equipamento, id_dispositivo_iot)
VALUES ('DEMO - Medição do motor', 'Ponto vinculado ao equipamento', @demo_agora,
        'Ativo', NULL, NULL, @demo_motor, @demo_esp1);
SET @demo_ponto_motor = LAST_INSERT_ID();

INSERT INTO tbl_ponto_medicao
(nome, descricao, data_instalacao, status, id_local, id_setor, id_equipamento, id_dispositivo_iot)
VALUES
('DEMO - Medição da reserva', 'Ponto sem histórico', @demo_agora, 'Ativo', NULL, NULL, @demo_bomba, @demo_esp2),
('DEMO - Medição da usinagem', 'Escopo de setor', @demo_agora, 'Ativo', NULL, @demo_setor, NULL, @demo_esp3),
('DEMO - Medição da planta', 'Escopo de local', @demo_agora, 'Ativo', @demo_local, NULL, NULL, @demo_esp3);
SET @demo_ponto_bomba = (
    SELECT id FROM tbl_ponto_medicao WHERE id_equipamento = @demo_bomba AND id_dispositivo_iot = @demo_esp2
);

INSERT INTO tbl_pzem (id_ponto_medicao, numero_serie, modelo, versao, data_instalacao, status)
VALUES (@demo_ponto_motor, 'WVDEMO-PZEM01', 'PZEM-004T', '3.0', CURRENT_DATE - INTERVAL 30 DAY, 'Ativo'),
       (@demo_ponto_bomba, 'WVDEMO-PZEM02', 'PZEM-004T', '3.0', CURRENT_DATE - INTERVAL 15 DAY, 'Ativo');
SET @demo_pzem = (SELECT id FROM tbl_pzem WHERE numero_serie = 'WVDEMO-PZEM01');

-- As quatro leituras recentes variam 1,350 kWh entre a primeira e a ultima.
-- Medicoes de ontem só entram no filtro Ontem ou em períodos que incluam ontem.
INSERT INTO tbl_medicao
(id_pzem, id_ponto_medicao, data_hora, tensao, corrente, potencia_ativa, energia_acumulada, frequencia, fator_potencia)
VALUES
(@demo_pzem, @demo_ponto_motor, @demo_ontem,                     218.00, 8.900, 1850.00, 12.400, 60.00, 0.950),
(@demo_pzem, @demo_ponto_motor, @demo_ontem + INTERVAL 1 HOUR,   220.10, 9.000, 1900.00, 13.000, 60.01, 0.955),
(@demo_pzem, @demo_ponto_motor, @demo_agora - INTERVAL 45 MINUTE, 219.70, 9.100, 1940.00, 14.050, 60.01, 0.960),
(@demo_pzem, @demo_ponto_motor, @demo_agora - INTERVAL 30 MINUTE, 220.40, 9.530, 2020.00, 14.550, 59.99, 0.964),
(@demo_pzem, @demo_ponto_motor, @demo_agora - INTERVAL 15 MINUTE, 220.90, 9.640, 2080.00, 15.060, 60.00, 0.969),
(@demo_pzem, @demo_ponto_motor, @demo_agora - INTERVAL  5 MINUTE, 221.10, 9.800, 2100.00, 15.400, 60.00, 0.970);
SET @demo_leitura = (
    SELECT id FROM tbl_medicao WHERE id_pzem = @demo_pzem AND data_hora = @demo_agora - INTERVAL 5 MINUTE
);

-- Uma tarifa de exemplo somente quando a empresa não tem tarifa ativa no período.
INSERT INTO tbl_tarifa (id_empresa, descricao, valor_kwh, data_inicial, data_final, status)
SELECT @demo_empresa, 'WVDEMO - Tarifa de exemplo', 0.8000, CURRENT_DATE - INTERVAL 30 DAY, CURRENT_DATE, 'Ativa'
WHERE NOT EXISTS (
    SELECT 1 FROM tbl_tarifa WHERE id_empresa = @demo_empresa AND status = 'Ativa'
      AND data_inicial <= CURRENT_DATE
      AND (data_final IS NULL OR data_final >= CURRENT_DATE - INTERVAL 30 DAY)
);

INSERT INTO tbl_limite
(descricao, grandeza, operador, valor, severidade, periodo_inicial, periodo_final, ativa, id_local, id_setor, id_equipamento)
VALUES ('WVDEMO - Tensão do motor', 'tensao', '>', 221.000, 'critico', CURRENT_DATE - INTERVAL 30 DAY,
        CURRENT_DATE + INTERVAL 30 DAY, TRUE, NULL, NULL, @demo_motor);
SET @demo_limite = LAST_INSERT_ID();

INSERT INTO tbl_tipo_alerta (nome, categoria, descricao)
VALUES ('WVDEMO - Alerta elétrico', 'energetico', 'Caso de demonstração energética'),
       ('WVDEMO - Alerta de ESP32', 'comunicacao', 'Caso de demonstração de conectividade');
SET @demo_tipo_energia = (SELECT id FROM tbl_tipo_alerta WHERE nome = 'WVDEMO - Alerta elétrico');
SET @demo_tipo_com = (SELECT id FROM tbl_tipo_alerta WHERE nome = 'WVDEMO - Alerta de ESP32');

INSERT INTO tbl_alerta
(id_tipo_alerta, id_limite, id_dispositivo_iot, id_medicao, valor_detectado, valor_limite,
 severidade, mensagem, data_hora, status, reconhecido_por, data_reconhecimento)
VALUES
(@demo_tipo_energia, @demo_limite, @demo_esp1, @demo_leitura, 221.100, 221.000,
 'critico', 'DEMO - Tensão acima do limite ilustrativo', @demo_agora - INTERVAL 5 MINUTE, 'ativo', NULL, NULL),
(@demo_tipo_com, NULL, @demo_esp2, NULL, NULL, NULL,
 'atencao', 'DEMO - Dispositivo reserva offline', @demo_agora - INTERVAL 20 MINUTE, 'ativo', NULL, NULL),
(@demo_tipo_com, NULL, @demo_esp1, NULL, NULL, NULL,
 'informativo', 'DEMO - Reconexão já reconhecida', @demo_agora - INTERVAL 40 MINUTE, 'reconhecido', @demo_admin, @demo_agora - INTERVAL 35 MINUTE);

INSERT INTO tbl_evento_conexao (id_dispositivo_iot, evento, data_hora, duracao_segundos)
VALUES (@demo_esp1, 'desconectado', @demo_agora - INTERVAL 50 MINUTE, 600),
       (@demo_esp1, 'reconectado', @demo_agora - INTERVAL 40 MINUTE, NULL),
       (@demo_esp2, 'desconectado', @demo_agora - INTERVAL 20 MINUTE, NULL);

COMMIT;

SELECT codigo, nome FROM tbl_local WHERE id = @demo_local;
SELECT numero_serie, id AS id_pzem FROM tbl_pzem WHERE numero_serie IN ('WVDEMO-PZEM01', 'WVDEMO-PZEM02');
SELECT id_pzem, data_hora, tensao, potencia_ativa, energia_acumulada
FROM tbl_medicao WHERE id_pzem = @demo_pzem ORDER BY data_hora;
