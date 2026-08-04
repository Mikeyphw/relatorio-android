package com.mikeyphw.relatorio.model

private fun field(id: String, label: String, custom: Boolean = false) =
    ReportFieldDefinition(id = id, labelPtBr = label, allowsCustomLabel = custom)

private fun section(id: String, title: String, vararg fields: ReportFieldDefinition) =
    ReportSectionDefinition(id = id, titlePtBr = title, fields = fields.toList())

object ReportTemplateCatalog {
    val standard = ReportTemplateDefinition(
        id = ReportTemplateId.AdTimoteo2026,
        movementIncludesBirthDate = true,
        sections = listOf(
            section(
                "ministers",
                "Ministros e obreiros",
                field("pastors", "Pastores"),
                field("evangelists", "Evangelistas"),
                field("elders", "Presbíteros"),
                field("deacons", "Diáconos"),
                field("deaconesses", "Diaconisas"),
                field("probationers", "Probatórios"),
            ),
            section(
                "members",
                "Total de membros",
                field("members", "Membros"),
                field("elderly", "Idosos"),
                field("youth", "Jovens"),
                field("teenagers", "Adolescentes"),
                field("children", "Crianças"),
                field("prayer_circle", "Círculo de oração"),
                field("transfer_received", "Carta de mudança recebida"),
                field("transfer_sent", "Carta de mudança expedida"),
                field("new_converts", "Novos convertidos"),
            ),
            section(
                "communion",
                "Santa Ceia",
                field("services", "Culto de Santa Ceia"),
                field("participants", "Participaram da Santa Ceia"),
                field("non_participants", "Não participaram da Santa Ceia"),
            ),
            section(
                "other",
                "Outros",
                field("conversions", "Almas que entregaram a Jesus"),
                field("child_presentations", "Apresentação de crianças"),
                field("pastoral_visits", "Visitas feitas pelo pastor"),
                field("sick_bedridden", "Enfermos e acamados"),
                field("other", "Outros"),
            ),
            section(
                "sunday_school_and_services",
                "Escola Bíblica Dominical e cultos",
                field("sunday_school_total", "EBD — Total matriculados"),
                field("sunday_school_adults", "EBD — Adultos"),
                field("sunday_school_youth", "EBD — Jovens"),
                field("sunday_school_teenagers", "EBD — Adolescentes"),
                field("sunday_school_children", "EBD — Crianças"),
                field("outdoor_services", "Cultos — Ar livre"),
                field("family_services", "Cultos — Familiar"),
                field("deliverance_services", "Cultos — Libertação"),
                field("mission_services", "Cultos — Missões"),
                field("other_services", "Cultos — Outros"),
            ),
        ),
    )

    val complete = ReportTemplateDefinition(
        id = ReportTemplateId.Complete,
        movementIncludesBirthDate = false,
        sections = listOf(
            standard.sections[0],
            standard.sections[1],
            standard.sections[2],
            section(
                "services",
                "Cultos",
                field("outdoor", "Ar livre"),
                field("revival", "Avivamento"),
                field("family", "Familiar"),
                field("deliverance", "Libertação"),
                field("missions", "Missões"),
                field("youth", "Jovens"),
                field("teenagers", "Adolescentes"),
                field("children", "Crianças"),
                field("public", "Público"),
                field("prayer_circle_meeting", "Encontro do círculo de oração"),
            ),
            section(
                "rehearsals",
                "Ensaios",
                field("youth", "Jovens"),
                field("teenagers", "Adolescentes"),
                field("children", "Crianças"),
                field("choir", "Coral"),
                field("prayer_circle", "Círculo de oração"),
                field("elderly", "Idosos"),
            ),
            section(
                "bible_studies",
                "Estudos bíblicos",
                field("seminars_1", "Seminários"),
                field("teenagers", "Adolescentes"),
                field("youth", "Jovens"),
                field("children", "Crianças"),
                field("whole_church", "Com toda a igreja"),
                field("seminars_2", "Seminários — segunda linha do formulário"),
            ),
            section(
                "other",
                "Outros",
                field("conversions", "Almas que entregaram a Jesus"),
                field("child_presentations", "Apresentação de crianças"),
                field("holy_spirit_baptisms", "Batismo com Espírito Santo"),
                field("water_baptisms", "Pessoas batizadas nas águas"),
                field("evangelistic_campaigns", "Campanha evangelística"),
                field("caravans", "Caravanas realizadas"),
                field("pre_baptism_course", "Curso pré-batismo"),
                field("funeral_services", "Culto fúnebre"),
                field("pastoral_visits", "Visitas feitas pelo pastor"),
            ),
            section(
                "meetings",
                "Reuniões",
                field("whole_church", "Com toda a igreja"),
                field("members", "Membros"),
                field("assistants", "Auxiliares"),
                field("workers", "Obreiros"),
                field("new_converts", "Novos convertidos"),
                field("youth", "Jovens"),
                field("teenagers", "Adolescentes"),
                field("custom_1", "Outro tipo de reunião 1", custom = true),
                field("custom_2", "Outro tipo de reunião 2", custom = true),
                field("custom_3", "Outro tipo de reunião 3", custom = true),
            ),
            section(
                "sunday_school",
                "Escola Bíblica Dominical",
                field("total", "Total matriculados"),
                field("adults", "Adultos"),
                field("youth", "Jovens"),
                field("teenagers", "Adolescentes"),
                field("children", "Crianças"),
                field("held", "E.B.D. realizadas"),
                field("teacher_study_children", "Estudo com professores — infantil"),
                field("teacher_study_adults", "Estudo com professores — adulto"),
            ),
            section(
                "prayer",
                "Oração",
                field("campaigns", "Campanha de oração"),
                field("women_circle", "Oração — círculo de oração feminino"),
                field("men_circle", "Oração — círculo de oração masculino"),
                field("whole_church", "Com toda a igreja"),
                field("teenagers", "Adolescentes"),
                field("youth", "Jovens"),
                field("vigil", "Vigília"),
            ),
        ),
    )

    val all: List<ReportTemplateDefinition> = listOf(standard, complete)

    fun definition(id: ReportTemplateId): ReportTemplateDefinition = when (id) {
        ReportTemplateId.AdTimoteo2026 -> standard
        ReportTemplateId.Complete -> complete
    }
}
