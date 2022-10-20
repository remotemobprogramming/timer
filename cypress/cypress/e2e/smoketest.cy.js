describe('Mob.sh Timer', () => {
  const roomId = 'testroom-310a9c47-515c-4ad7-a229-ae8efbab7387';

  it('index page is available', () => {
    cy.visit('https://timer.mob.sh')
    cy.contains('Mob Timer')
  })

  it('room page is available', () => {
    cy.visit('https://timer.mob.sh/' + roomId)
    cy.contains('#' + roomId)
  })

  it('help page is available', () => {
    cy.visit('https://timer.mob.sh/help' + roomId)
    cy.contains('Help')
  })

  it('stats page is available', () => {
    cy.visit('https://timer.mob.sh/help' + roomId)
    cy.contains('Help')
  })

  it('mob timer works', () => {
    cy.visit('https://timer.mob.sh/' + roomId)
    cy.get('[data-bs-toggle="modal"]').click()
    cy.get('#timer-input').clear().type("1")
    cy.wait(100)
    cy.get('.btn-close').click()
    cy.get('#timer-button').contains(1).click()
    cy.get('#timer').should('not.contain', '00:00')
    cy.get('#timer-type').should('not.contain', '☕')
  })

  it('break timer works', () => {
    cy.visit('https://timer.mob.sh/' + roomId)
    cy.get('[data-bs-toggle="modal"]').click()
    cy.get('#breaktimer-input').clear().type("1")
    cy.wait(100)
    cy.get('.btn-close').click()
    cy.get('#breaktimer-button').contains(1).click()
    cy.get('#timer').should('not.contain', '00:00')
    cy.get('#timer-type').contains('☕')
  })

  it('history increases for timer button', () => {
    cy.visit('https://timer.mob.sh/' + roomId)
    cy.get('[data-bs-toggle="modal"]').click()
    cy.get('#timer-input').clear().type("1")
    cy.wait(100)
    cy.get('.btn-close').click()
    let initialHistoryCountChainer = cy.get('#history-container').find('li').its('length')
    cy.get('#timer-button').contains(1).click().wait(200)
    cy.get('#history-container').find('li').its('length').then(currentCount => {
      initialHistoryCountChainer.should('eq', currentCount - 1)
    })
  })

  it('history increases for breaktimer button', () => {
    cy.visit('https://timer.mob.sh/' + roomId)
    cy.get('[data-bs-toggle="modal"]').click()
    cy.get('#breaktimer-input').clear().type("1")
    cy.wait(100)
    cy.get('.btn-close').click()
    let initialHistoryCountChainer = cy.get('#history-container').find('li').its('length')
    cy.get('#breaktimer-button').contains(1).click().wait(200)
    cy.get('#history-container').find('li').its('length').then(currentCount => {
      initialHistoryCountChainer.should('eq', currentCount - 1)
    })
  })
})
