package med.voll.api.domain.consulta;

import med.voll.api.domain.ValidacaoException;
import med.voll.api.domain.consulta.validacoes.agendamento.ValidadorAgendamentoConsulta;
import med.voll.api.domain.consulta.validacoes.cancelamento.ValidadorCancelamentoConsulta;
import med.voll.api.domain.medico.Medico;
import med.voll.api.domain.medico.MedicoRepository;
import med.voll.api.domain.paciente.PacienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AgendaConsultas {
    @Autowired
    private ConsultaRepository repository;
    @Autowired
    private MedicoRepository medicoRepository;
    @Autowired
    private PacienteRepository pacienteRepository;
    @Autowired
    private List<ValidadorAgendamentoConsulta> validadores;

    @Autowired
    private List<ValidadorCancelamentoConsulta> validadoresCancelamento;

    public DadosDetalhamentoConsulta agendar(DadosAgendamentoConsulta dados){
        if(!this.pacienteRepository.existsById(dados.idPaciente())){
            throw new ValidacaoException("Id do paciente informado não existe!");
        }
        if(dados.idMedico() != null && !this.medicoRepository.existsById(dados.idMedico())){
            throw new ValidacaoException("Id do médico informado não existe!");
        }

        this.validadores.forEach(v -> v.validar(dados));

        var paciente = this.pacienteRepository.getReferenceById(dados.idPaciente());
        var medico = escolherMedico(dados);
        if(medico == null){
            throw new ValidacaoException("Não existe médico disponível nesta data!");
        }
        var consulta = new Consulta(null, medico, paciente, dados.data(), null);
        this.repository.save(consulta);

        return new DadosDetalhamentoConsulta(consulta);
    }

    private Medico escolherMedico(DadosAgendamentoConsulta dados) {
        if (dados.idMedico() != null) {
            return this.medicoRepository.getReferenceById(dados.idMedico());
        }
        if(dados.especialidade() == null){
            throw new ValidacaoException("Especialidade é obrigatória quando médico não for escolhido!");
        }
        return this.medicoRepository.escolherMedicoAleatorioLivreNaData(dados.especialidade(), dados.data());
    }

    public void cancelar(DadosCancelamentoConsulta dados) {
        if(!this.repository.existsById(dados.idConsulta())){
            throw new ValidacaoException("Id da consulta informada não existe!");
        }
        this.validadoresCancelamento.forEach((v -> v.validar(dados)));

        var consulta = this.repository.getReferenceById(dados.idConsulta());
        consulta.cancelar(dados.motivo());
    }
}
