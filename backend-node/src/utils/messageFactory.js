const { v4: uuidv4 } = require('uuid');

function createTextMessage(sender, receiver, content) {
  return {
    id: uuidv4(),
    sender,
    receiver,
    content,
    timestamp: new Date().toISOString(),
    status: 'SENT'
  };
}

module.exports = { createTextMessage };