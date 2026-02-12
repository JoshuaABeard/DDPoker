/*
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * DD Poker - Terms of Use Page
 * =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 */

import { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Terms of Use - DD Poker Community Edition',
  description: 'Terms of use for DD Poker Community Edition online game service',
}

export default function Terms() {
  return (
    <div className="container mx-auto px-4 py-8 max-w-4xl">
      <h1 className="text-3xl font-bold mb-4">DD Poker Terms of Use</h1>

      <div className="leading-relaxed space-y-4">
        <p>
          Use of the DD Poker online game service is free, but subject to some reasonable requests. Because you will
          potentially be playing with people you don't know, we ask for a certain level of courtesy and respect. We
          require that you:
        </p>

        <ul className="list-disc list-inside space-y-2 ml-4">
          <li>
            Do not use profanity or inappropriate language in the chat room or as part of your online profile name.
          </li>
          <li>
            Do not insult, berate or act in a manner considered disrespectful or rude to other players. In short, be
            nice and friendly in your communications or don't say anything at all.
          </li>
        </ul>

        <p>
          If you do not abide by these terms, we reserve the right to deactivate your online profile and to prevent
          future use of this service.
        </p>

        <p>We thank you for your cooperation and help in keeping the DD Poker Online Games Portal a fun experience.</p>

        <p>We reserve the right to change the terms of service at any time.</p>
      </div>
    </div>
  )
}
